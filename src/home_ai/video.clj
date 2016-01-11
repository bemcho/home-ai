(ns home-ai.video

  (:import javax.imageio.ImageIO)
  (:import (java.io FileOutputStream DataOutputStream File))
  (:import javax.swing.JFrame
           javax.swing.JPanel
           java.awt.FlowLayout
           org.opencv.videoio.VideoCapture
           )

  (:require  [home-ai.opencv :refer :all]
            ))

;This can records raw video to a file called stream.m4v
;To convert to video use
;ffmpeg -f h264 -an -i vid.h264 stream.m4v



;;;;;

(def window (JFrame. "test"))
(def view (JPanel.))
(def stream (atom true))
(def video-agent (agent {}))
(def save-video (atom false))
(def opencv (atom true))

(defn configure-save-video [b]
  (reset! save-video b))

(defn configure-opencv [b]
  (reset! opencv b))

(defn setup-viewer []
  (def window (JFrame. "test"))
  (def view (JPanel.))
  (doto window
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.setBounds 30 30 900 950)
    )
  (.add (.getContentPane window) view)
  (.setVisible window true)
  (def g (.getGraphics view)))

(defn update-image [bi]
  (do
                                         (.drawImage g bi 10 10 view)
    ;(q/image bi 0 0) 
    )
  )

(defn save-image [bi]
  (ImageIO/write bi "png" (File. "opencvin.png")))

(defn display-frame [matImg]
  (try
    (update-image
      (process-mat-and-return-image matImg))
    (catch Exception e (println (str "Error displaying frame - skipping " e)))))

(defn write-payload [video out]
  (let [barray (byte-array (* (.total video) (.channels video)))]

    (do
      (.get video 0 0 barray)
    (.write out  barray))))

(defn read-frame [cam out]
  (try
    (do
      (let [frame (capture-from-cam cam)]
        (when out
          (write-payload frame out))
        (display-frame frame)))
    (catch Exception e (println (str "Problem reading frame - skipping " e)))))

(defn stream-video [_ cam out]
  (if @stream (do
                  (read-frame cam out)
                  (send video-agent stream-video cam  (when @save-video
                                                                         out)))
              (.release cam))
  )


(defn end-video []
  (reset! stream false))



(defn init-video []
  (setup-viewer))

(defn start-video [device]
  (do
    (reset! stream true)
    (Thread/sleep 40)
    ;wait for the first frame
    (send video-agent stream-video (VideoCapture. device) (when @save-video
                                            (FileOutputStream. "vid.h264")))))

     (comment
       (do
         (init-opencv)
         (init-video )
         (start-video 0))
       (end-video))

