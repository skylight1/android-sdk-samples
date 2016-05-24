**ServiceFrameDetectorDemo**

This sample app demonstrates how to continuously collect preview frames from the camera and feed them to the Affdex SDK's FrameDetector for processing, without showing the camera preview in the app's UI.

The integration with the camera and the Affdex SDK takes place in a background thread managed by a Service (DetectorService).  The Service's lifetime is controlled by an Application subclass (DemoApplication), which tracks when Activities start and stop, starting the Service whenever the number of started Activities transitions from 0 to 1, and stopping it when the number of started Activities transitions from 1 to 0.  As a result, the Service is running whenever the app is currently showing its UI, but not when another app is on display.

When the Service is notified by the Affdex FrameDetector that a face has been found, it displays a toast message confirming that.  It also confirms when the face has been lost.  In addition, the Service configures the FrameDetector to return valence scores, and write those scores to the Android log.

As demonstration of the fact that the Affdex processing continues independently of Activity transitions in the app, the main Activity shows a button which launches a second activity.  By observing the log output during Activity changes, you can see that this processing continues without interruption.

