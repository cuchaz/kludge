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

	override fun close() {
		for (thing in things.reversed()) {
			thing.close()
		}
	}
}

fun <T:AutoCloseable> T.autoClose(closer: AutoCloser): T = closer.add(this)
