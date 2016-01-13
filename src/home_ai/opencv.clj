(ns home-ai.opencv
  (:import
    org.opencv.core.Core
    org.opencv.core.MatOfRect
    org.opencv.core.MatOfByte
    org.opencv.core.Point
    org.opencv.core.Rect
    org.opencv.core.Mat
    org.opencv.core.Scalar
    org.opencv.imgcodecs.Imgcodecs
    org.opencv.objdetect.CascadeClassifier
    org.opencv.objdetect.Objdetect
    org.opencv.videoio.VideoCapture
    java.awt.image.BufferedImage
    java.awt.RenderingHints
    javax.imageio.ImageIO
    java.io.File
    java.io.ByteArrayInputStream
    org.opencv.core.Size
    (org.opencv.imgproc Imgproc)
    (org.opencv.face Face)
    (org.opencv.core CvType)
    (org.opencv.highgui Highgui))
    (:require [clojure.walk :as walk]))


(defn convert-mat-to-buffer-image [mat]
  (let [new-mat (MatOfByte.)]
    (Imgcodecs/imencode ".png" mat new-mat)
    (ImageIO/read (ByteArrayInputStream. (.toArray new-mat)))))

(def all-detections-for-image (atom []))
(def face-detections (atom []))
(def trainning-samples (atom (vector)))
(def training (atom false))
(def collect-samples (atom false))
(def empirical-sample-count 100)
(def confidence 123.0)
(def traning-rectangle (atom (Rect. 300 100 250  250)) )
(def classifiers-path "resources/data/classifiers/")
(def recognizers-path "resources/data/facerecognizers/lbphFaceRecognizer.xml")
(def label (atom 3))
(def train-agent (agent {}))
(def recognize-agent (agent {:label -1}))
(declare lbph-face-recognizer)
(declare classifiers)


(defn create-classifier-agent [classifier]
  (agent {:detections (MatOfRect.)
          :classifier classifier}))

(defn load-classifier [file-path]
  (walk/keywordize-keys (assoc {} (last (clojure.string/split file-path #"/")) (create-classifier-agent (CascadeClassifier.
                                                                                                          file-path)))))

(defn load-image [filename]
  (Imgcodecs/imread filename))

(defn detect-faces! [classifier image]
  (.detectMultiScale classifier
                     image
                     @face-detections))

(defn detect-faces-agent! [a image]
  (.detectMultiScale (:classifier a)
                     image
                     (:detections a)
                     )
  ;(reset! all-detections-for-image (concat @all-detections-for-image (.toArray @detections)))
  a
    )
(defn recognize
  "docstring"
  [a mat]
  (:label (.predict @lbph-face-recognizer mat))
  (:mat mat)
  a
  )
(defn draw-bounding-boxes!
  [image]
  (let [imageMatGray (Mat.)]
    (Imgproc/cvtColor image imageMatGray Imgproc/COLOR_BGR2GRAY)
    ;(Imgproc/equalizeHist imageMatGray imageMatGray)
    (doall
      (map (fn [rect]
             (Imgproc/rectangle image
                                (Point. (.x rect) (.y rect))
                                (Point. (+ (.x rect) (.width rect))
                                        (+ (.y rect) (.height rect)))
                                (Scalar. 0 255 0) 2))
           (concat @all-detections-for-image (when @training (vector  @traning-rectangle)))))

    (doall
      (map (fn [rect]
             (let [predictedLabel (.predict @lbph-face-recognizer (Mat. (.clone imageMatGray)  rect  ) )]
               (Imgproc/putText image (str "Label=" (if (and (>= predictedLabel 0) (<= predictedLabel @label))  predictedLabel predictedLabel) )
               (Point. (.x rect) (- (.y rect) 10))
               Highgui/CV_FONT_NORMAL  1 (Scalar. 255 255 51) 2)))
           @all-detections-for-image)
               )
             )

      (when @training
        (Imgproc/putText image (str "Train for Label=" @label)  (Point. (.x @traning-rectangle) (- (.y @traning-rectangle) 10) )  Highgui/CV_FONT_NORMAL  1 (Scalar. 0 0 204) 2))
      ;(Imgcodecs/imwrite "faceDetections.png" image)
      (convert-mat-to-buffer-image image))



(defn load-classifiers
  [dir-name]
  (loop [file-paths (filter #(.endsWith (.getName %) ".xml") (.listFiles (java.io.File. dir-name)) )
         result {}]
    (if-not (first file-paths)
      result
      (recur
        (rest file-paths)
        (merge result (load-classifier (str dir-name (.getName (first file-paths)))))))))



(defn process-mat-and-return-image [imageMat]
  (reset! all-detections-for-image [])
  (let [imageMatGray (Mat.)
        agents (vals @classifiers)]
    (Imgproc/cvtColor imageMat imageMatGray Imgproc/COLOR_BGR2GRAY)
    (Imgproc/equalizeHist imageMatGray imageMatGray)
    (dorun
      (if @training
        (reset! trainning-samples (concat @trainning-samples (vector (.clone (Mat. imageMatGray @traning-rectangle)))))
        (doseq [agent agents]
          (send agent detect-faces-agent! imageMatGray )
          (doall (map #(reset! all-detections-for-image (concat @all-detections-for-image (.toArray (:detections (deref %)))  ))   agents))))
      )
        )
    (draw-bounding-boxes! imageMat))

  (defn capture-from-cam
    "Gets frame from cam and returns it as Mat."
    [cam]
    (let [matImage (Mat.)]
      (when (.isOpened cam)
        (do
          (.read cam matImage))) matImage))

(defn capture-from-device [n]
  (capture-from-cam (VideoCapture. n)))
;(process-and-return-image "opencvin.png")

(defn toggle-collect-samples []
  (reset! collect-samples (not @collect-samples)))

(defn toggle-training
  "docstring"
  []
  (reset! training (not @training)))

(defn init-opencv []
  (def classifiers (atom (load-classifiers classifiers-path)))
  (def lbph-face-recognizer (atom (Face/createLBPHFaceRecognizer 1 8 8 8 confidence)))
  (if (.exists (File. recognizers-path))
    (dosync
      (.load @lbph-face-recognizer recognizers-path))
    )
  )

(defn update-lbph-recognizer
  ""
  [samples]
  (do
    (let [samples-count (.size @trainning-samples)
          mat (atom (Mat/zeros  1 samples-count CvType/CV_32SC1)) ]
      (dosync
        (doall
          (map #(fn [%1 %2] (do (.put @mat 0 %1  %2))) (range samples-count ) (repeat samples-count @label))
        )
        (.update @lbph-face-recognizer samples @mat)
        (.save @lbph-face-recognizer recognizers-path)
        (reset! trainning-samples [])
        (reset! label (+ @label 1)))
      )
    ))

(defn update-recognizer
  "docstring"
  [a matSamples]
  (do
    (toggle-collect-samples)
    (toggle-training))
    (update-lbph-recognizer matSamples)
a
  )

(defn start-training []
  (reset! training true)
  (reset! collect-samples true)
  )
