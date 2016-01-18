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
    (start-visual-repl 0)
    ; A button with an action listener that will cause the dialog to close
    ; and return :ok to the invoker.
    )
)


