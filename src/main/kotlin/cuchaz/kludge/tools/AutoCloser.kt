/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.tools


/**
 * Keeps track to AutoCloseable instances and closes them in LIFO order
 */
class AutoCloser : AutoCloseable {

	private val things = ArrayList<AutoCloseable>()

	fun <T:AutoCloseable> add(thing: T, replace: T? = null): T {

		// if we're replacing something, remove that and close it
		if (replace != null) {
			val wasRemoved = remove(replace)
			assert (wasRemoved) {
				"$replace was not in the autoclean list"
			}
			replace.close()
		}

		// don't allow closing the same thing twice
		assert (thing !in things) {
			"$thing is already in the autoclean list"
		}

		// add the new thing
		things.add(thing)

		return thing
	}

	fun <T:AutoCloseable> remove(thing: T) = things.remove(thing)

	fun add(block: () -> Unit) {
		things.add(AutoCloseable { block() })
	}

	override fun close() {
		for (thing in things.reversed()) {
			thing.close()
		}
	}
}

interface WithAutoCloser {
	fun <T:AutoCloseable> T.autoClose(replace: T? = null): T
	fun autoClose(block: () -> Unit)
}

inline fun <T> autoCloser(block: WithAutoCloser.() -> T): T =
	AutoCloser().use { autoCloser ->
		object : WithAutoCloser {

			override fun <T:AutoCloseable> T.autoClose(replace: T?): T = apply {
				autoCloser.add(this, replace)
			}

			override fun autoClose(block: () -> Unit) =
				autoCloser.add(block)

		}.block()
	}
