/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge

import cuchaz.kludge.window.Monitors
import cuchaz.kludge.window.Size
import cuchaz.kludge.window.Window
import cuchaz.kludge.window.Windows


fun main(args: Array<String>) {

	// listen to GLFW error messages
	Windows.init()
	Windows.errors.setOut(System.err)

	Window(
		size = Size(640, 480),
		title = "Kludge Demo"
	).use { win ->

		win.centerOn(Monitors.primary)
		win.visible = true

		// main loop
		while (!win.shouldClose()) {

			// TODO: render something

			Windows.pollEvents()
		}
	}

	// cleanup
	Windows.close()
}
