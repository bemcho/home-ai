(ns home-ai.core
  (:require
             [home-ai.video :refer :all]
             [home-ai.opencv :refer :all]
            )
  (:gen-class))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    ;(clojure.lang.RT/loadLibrary org.opencv.core.Core/NATIVE_LIBRARY_NAME)
    (start-visual-repl)
    ; A button with an action listener that will cause the dialog to close
    ; and return :ok to the invoker.
    (comment
      (.setLabelInfo @lbph-face-recognizer 1 "Emil Tomov")
      (.setLabelInfo @lbph-face-recognizer 0 "Unknown")
      (.setLabelInfo @lbph-face-recognizer 3 "Venceslav Marinov")
      (.setLabelInfo @lbph-face-recognizer 2 "Deyan Rizov")
      (.setLabelInfo @lbph-face-recognizer 4 "Svetoslav Kolev"))
    )
    )



