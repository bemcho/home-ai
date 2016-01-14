(ns home-ai.video

  (:import javax.imageio.ImageIO)
  (:import (java.io FileOutputStream DataOutputStream File))
  (:import javax.swing.JFrame
           javax.swing.JPanel
           java.awt.FlowLayout
           org.opencv.videoio.VideoCapture
           (javax.swing JButton JTextField JLabel JList JScrollPane)
           (java.awt GridLayout Dimension)
           (java.awt.event ActionListener)
           (javax.swing.event ListSelectionListener))

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

(defn read-labels-from-mat  [mat]
  (into-array String (map #(str %1) [1 2 3 4 5 6 7 8 9 10 11]))
  )
(defn setup-viewer []
  (def window (JFrame. "test"))
  (def view (JPanel.))
  (doto window
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.setBounds 0 0 1300 950)
    )


  (import 'java.awt.event.ActionListener)

  (def layout (GridLayout. 2 0))
  (def train-panel-layout (GridLayout. 0 1))

  (def view-panel-layout (GridLayout. 0 1))
  (.setLayout view view-panel-layout)



  (def label-label (JLabel. "Enter label number you want to associate with label info.You can add different labels with same label info."))
  (def input-label (JList. (read-labels-from-mat (.getLabels @lbph-face-recognizer))))
  (.setVisibleRowCount input-label 4)
  (def scroll  (JScrollPane.))
  (doto scroll
    (.setViewportView  input-label)
    (.setVisible  true)
    (.setMinimumSize(Dimension. 50 50)))


  (def label-label-info (JLabel. "Enter String representing your label {first last name etc.}"))
  (def input-label-info (JTextField. (.getLabelInfo @lbph-face-recognizer (parse-int (if (.getSelectedValue input-label) (.getSelectedValue input-label) "1")) )))

  (def on-change-label (proxy [ListSelectionListener] []
                         (valueChanged [event]
                           (.setText input-label-info  (.getLabelInfo @lbph-face-recognizer
                                                                      (parse-int (if (.getSelectedValue input-label) (.getSelectedValue input-label) "1")) )))))
  (.addListSelectionListener input-label on-change-label)

  (def training-panel (JPanel.))
  (doto training-panel
    (.setLayout train-panel-layout)
    (.add scroll))

  (.setLayout (.getContentPane window) layout)
  (.add (.getContentPane window) view)

  (.add training-panel label-label-info)
  (.add training-panel input-label-info)

  (.add training-panel label-label)
  (.add training-panel input-label)



  (def training-button (JButton. "Start Training"))
  (def act (proxy [ActionListener] []
             (actionPerformed [event] (start-training (.getText input-label-info) (parse-int (.getSelectedValue input-label)) ))))

  (.addActionListener training-button act)
  (.add training-panel training-button)

  (.add (.getContentPane window) training-panel)
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
                  (when (and @collect-samples (>= (.size @trainning-samples ) empirical-sample-count))
                    (send-off train-agent update-recognizer @trainning-samples))
                  (send video-agent stream-video cam  (when @save-video
                                                        out))
                  )

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

     (defn start-visual-repl
       []
       (do
         (init-opencv)
         (init-video )
         (start-video 0))
       )

