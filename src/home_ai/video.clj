(ns home-ai.video

  (:import javax.imageio.ImageIO)
  (:import (java.io FileOutputStream File))
  (:import javax.swing.JFrame
           javax.swing.JPanel
           org.opencv.core.Mat
           org.opencv.videoio.VideoCapture
           org.opencv.videoio.Videoio
           (javax.swing JButton JTextField JLabel JList JScrollPane BoxLayout DefaultListSelectionModel DefaultListModel JComponent)
           (java.awt GridLayout Dimension)
           (java.awt.event ActionListener)
           (javax.swing.event ListSelectionListener)
           (java.awt.image BufferedImage))

  (:require [home-ai.opencv :refer :all]
            ))

;This can records raw video to a file called stream.m4v
;To convert to video use
;ffmpeg -f h264 -an -i vid.h264 stream.m4v



;;;;;

(def window-training (JFrame. "test"))
(def window-view (JFrame. "test"))
(def view (JPanel.))
(def stream (atom true))
(def video-agent (agent {}))
(def save-video (atom false))
(def opencv (atom true))

(defn configure-save-video [b]
  (reset! save-video b))

(defn configure-opencv [b]
  (reset! opencv b))

(defn read-labels-from-mat [mat]
  (map #(str %1) (range 1 100))
  )
(defn setup-viewer []
  (def window-training (JFrame. "test"))
  (def view (JPanel.))
  (.setPreferredSize view (Dimension. 1000 1000))
  (def view-panel-layout (BoxLayout. view BoxLayout/Y_AXIS))
  (.setLayout view view-panel-layout)
  (.setAlignmentX view JComponent/CENTER_ALIGNMENT)
  (doto window-view
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.setBounds 0 0 700 700)
    )

  (doto window-training
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.setBounds 700 0 500 500)
    )
  window-view

  (import 'java.awt.event.ActionListener)

  (def layout (GridLayout. 2 0))






  (def label-label (JLabel. "Enter label number you want to associate with label info.You can add different labels with same label info."))
  (.setAlignmentX label-label JComponent/CENTER_ALIGNMENT)
  (def label-list-model (DefaultListModel.))
  (dorun
    (map (fn [e] (.addElement label-list-model e)) (read-labels-from-mat (.getLabels @lbph-face-recognizer))))

  (def input-label (JList. label-list-model))
  (.setAlignmentX input-label JComponent/CENTER_ALIGNMENT)
  (.setPreferredSize input-label (Dimension. 10 200))
  (.setVisibleRowCount input-label -1)
  (.setSelectionMode input-label DefaultListSelectionModel/SINGLE_INTERVAL_SELECTION)
  (.setLayoutOrientation input-label JList/HORIZONTAL_WRAP)
  (.setSelectionInterval input-label 0 0)

  (def label-list-scroller (JScrollPane.))
  (doto label-list-scroller
    (.setViewportView input-label)
    (.setPreferredSize (Dimension. 250 80))
    (.setAlignmentX JComponent/CENTER_ALIGNMENT)
    )


  (def label-label-info (JLabel. "Enter String representing your label {first last name etc.}"))
  (.setAlignmentX label-label-info JComponent/CENTER_ALIGNMENT)

  (def input-label-info (JTextField. (.getLabelInfo @lbph-face-recognizer (parse-int (if (.getSelectedValue input-label) (.getSelectedValue input-label) "1")))))
  (.setPreferredSize input-label-info (Dimension. 70 40))
  (.setAlignmentX input-label-info JComponent/CENTER_ALIGNMENT)

  (def on-change-label (proxy [ListSelectionListener] []
                         (valueChanged [event]
                           (.setText input-label-info (.getLabelInfo @lbph-face-recognizer
                                                                     (parse-int (if (.getSelectedValue input-label) (.getSelectedValue input-label) "1")))))))
  (.addListSelectionListener input-label on-change-label)

  (def training-panel (JPanel.))
  (def train-panel-layout (BoxLayout. training-panel BoxLayout/Y_AXIS))
  (doto training-panel
    (.setLayout train-panel-layout)
    )

  (def training-button (JButton. "Start Training"))
  (.setPreferredSize training-button (Dimension. 800 100))
  (.setAlignmentX training-button JComponent/CENTER_ALIGNMENT)
  (def act (proxy [ActionListener] []
             (actionPerformed [event]
               (.setEnabled training-button false)
               (start-training (.getText input-label-info) (parse-int (.getSelectedValue input-label))))))

  (.addActionListener training-button act)
  (.add training-panel training-button)

  (.add training-panel label-label-info)
  (.add training-panel input-label-info)

  (.add training-panel label-label)
  (.add training-panel label-list-scroller)

  (.setLayout (.getContentPane window-training) layout)
  (.add (.getContentPane window-view) view)
  (.add (.getContentPane window-training) training-panel)
  (.setVisible window-view true)
  (.setVisible window-training true)
  (.pack window-training)

  (def g (.getGraphics view)))

(defn update-image [^BufferedImage bi]
  (do
    (.drawImage g bi 10 10 view)
    ;(q/image bi 0 0) 
    )
  )

(defn save-image [bi]
  (ImageIO/write bi "png" (File. "opencvin.png")))

(defn display-frame [^Mat matImg]
  (try
    (update-image
      (process-mat-and-return-image matImg))
    (when (and (not @training) (not (.isEnabled training-button)))
      (.setEnabled training-button true)
      )
    (catch Exception e (println (str "Error displaying frame - skipping " e)))))

(defn write-payload [video out]
  (let [barray (byte-array (* (.total video) (.channels video)))]

    (do
      (.get video 0 0 barray)
      (.write out barray))))

(defn read-frame [^VideoCapture cam out]
  (try
    (do
      (let [frame (capture-from-cam cam)]
        (when out
          (write-payload frame out))
        (display-frame frame)))
    (catch Exception e (println (str "Problem reading frame - skipping " e)))))


(defn stream-video [_ ^Mat cam out]
  (if @stream (do
                (read-frame cam out)
                (when (and @collect-samples (>= (.size @trainning-samples) empirical-sample-count))
                  (send-off train-agent update-recognizer @trainning-samples))
                (send video-agent stream-video cam (when @save-video
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
    (let [cam (if (number? device)
                (do (VideoCapture.    device                      ;(+ device Videoio/CAP_FFMPEG Videoio/CAP_PROP_CONVERT_RGB)
                      ))
                ;else
                (do (VideoCapture. device                   ;(+ Videoio/CAP_FFMPEG Videoio/CAP_PROP_CONVERT_RGB)
                                   )))
          ]
      (doto cam
        (.set Videoio/CAP_PROP_FRAME_COUNT 30)
        ;(.set Videoio/CV_CAP_PROP_FRAME_WIDTH 1280)
        ;(.set Videoio/CV_CAP_PROP_FRAME_HEIGHT 720)
        )
      (send video-agent stream-video cam (when @save-video
                                           (FileOutputStream. "vid.h264"))))
    )
  )

(defn start-visual-repl
  [device]
  (do
    (init-opencv)
    (init-video)
    (start-video device))
  )

