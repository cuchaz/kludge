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

	fun <T:AutoCloseable> add(thing: T): T {
		things.add(thing)
		return thing
	}

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
	fun <T:AutoCloseable> T.autoClose(): T
	fun autoClose(block: () -> Unit)
}

inline fun <T> autoCloser(block: WithAutoCloser.() -> T): T =
	AutoCloser().use { autoCloser ->
		object : WithAutoCloser {

			override fun <T:AutoCloseable> T.autoClose(): T =
				autoCloser.add(this)

			override fun autoClose(block: () -> Unit) =
				autoCloser.add(block)

		}.block()
	}
