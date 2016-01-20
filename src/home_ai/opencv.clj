(ns home-ai.opencv
  (:import
    org.opencv.core.MatOfRect
    org.opencv.core.MatOfByte
    org.opencv.core.Point
    org.opencv.core.Rect
    org.opencv.core.Mat
    org.opencv.core.Scalar
    org.opencv.imgcodecs.Imgcodecs
    org.opencv.objdetect.CascadeClassifier
    org.opencv.videoio.VideoCapture
    javax.imageio.ImageIO
    java.io.ByteArrayInputStream
    (org.opencv.imgproc Imgproc)
    (org.opencv.face Face)
    (org.opencv.core CvType)
    (org.opencv.highgui Highgui)
    (java.io File)
    (java.awt.image BufferedImage))
  (:require [clojure.walk :as walk]))


(defn convert-mat-to-buffer-image [^Mat mat]
  (let [new-mat (MatOfByte.)]
    (Imgcodecs/imencode ".png" mat new-mat)
    (ImageIO/read (ByteArrayInputStream. (.toArray new-mat)))))

(def all-detections-for-image (atom []))
(def face-detections (atom []))
(def trainning-samples (atom (vector)))
(def training (atom false))
(def collect-samples (atom false))
(def empirical-sample-count 15)
(def confidence 123.0)
(def traning-rectangle (atom (Rect. 300 100 250 250)))
(def classifiers-path "resources/data/classifiers/")
(def recognizers-path "resources/data/facerecognizers/lbphFaceRecognizer.xml")
(def label (atom 1))
(def label-info (atom ""))
(def train-agent (agent {}))
(def save-model-agent (agent {:path recognizers-path}))
(declare lbph-face-recognizer)
(declare classifiers)


(defn create-classifier-agent [^CascadeClassifier classifier]
  (agent {:detections (MatOfRect.)
          :classifier classifier}))

(defn load-classifier [^String file-path]
  (walk/keywordize-keys (assoc {} (last (clojure.string/split file-path #"/")) (create-classifier-agent (CascadeClassifier.
                                                                                                          file-path)))))

(defn load-image [^String filename]
  (Imgcodecs/imread filename))

(defn detect-faces! [^CascadeClassifier classifier ^Mat image]
  (.detectMultiScale classifier
                     image
                     @face-detections))

(defn detect-faces-agent! [a ^Mat image]
  (.detectMultiScale (:classifier a)
                     image
                     (:detections a)
                     )
  ;(reset! all-detections-for-image (concat @all-detections-for-image (.toArray @detections)))
  a
  )
(defn recognize
  "docstring"
  [a ^Mat mat]
  (:label (.predict @lbph-face-recognizer mat))
  (:mat mat)
  a
  )
(defn ^BufferedImage draw-bounding-boxes!
  [^Mat image]
  (let [imageMatGray (Mat.)]
    (Imgproc/cvtColor image imageMatGray Imgproc/COLOR_BGR2GRAY)
    (Imgproc/equalizeHist imageMatGray imageMatGray)
    (doall
      (map (fn [^Rect rect]
             (Imgproc/rectangle image
                                (Point. (.x rect) (.y rect))
                                (Point. (+ (.x rect) (.width rect))
                                        (+ (.y rect) (.height rect)))
                                (Scalar. 0 255 0) 2))
           (concat @all-detections-for-image (when @collect-samples (vector @traning-rectangle)))))

    (doall
      (map (fn [^Rect rect]
             (let [detectedImageGray (.submat imageMatGray rect)
                   predictedLabel (.predict @lbph-face-recognizer detectedImageGray)
                   labelInfo (.getLabelInfo @lbph-face-recognizer predictedLabel)
                   ]
               (Imgproc/putText image (if (> predictedLabel 0) (str labelInfo "L:" predictedLabel) "Unknown")
                                (Point. (.x rect) (- (.y rect) 10))
                                Highgui/CV_FONT_NORMAL 1 (Scalar. 255 255 51) 2)))
           @all-detections-for-image)
      )
    )
  (when @training
    (Imgproc/putText image (str "Training for L=" @label) (Point. (.x @traning-rectangle) (- (.y @traning-rectangle) 40)) Highgui/CV_FONT_NORMAL 1 (Scalar. 0 0 204) 1))

  (when @collect-samples
    (Imgproc/putText image (str "Gather Samples for L=" @label) (Point. (.x @traning-rectangle) (- (.y @traning-rectangle) 10)) Highgui/CV_FONT_NORMAL 1 (Scalar. 0 0 204) 1))
  ;(Imgcodecs/imwrite "faceDetections.png" image)
  (convert-mat-to-buffer-image image))



(defn load-classifiers
  [^String dir-name]
  (loop [file-paths (filter #(.endsWith (.getName %) ".xml") (.listFiles (java.io.File. dir-name)))
         result {}]
    (if-not (first file-paths)
      result
      (recur
        (rest file-paths)
        (merge result (load-classifier (str dir-name (.getName (first file-paths)))))))))



(defn ^BufferedImage process-mat-and-return-image [^Mat imageMat]
  (reset! all-detections-for-image [])
  (let [imageMatGray (Mat.)
        agents (vals @classifiers)]
    (Imgproc/cvtColor imageMat imageMatGray Imgproc/COLOR_BGR2GRAY)
    (Imgproc/equalizeHist imageMatGray imageMatGray)
    (dorun
      (if @collect-samples
        (reset! trainning-samples (concat @trainning-samples (vector (.clone (.submat imageMatGray @traning-rectangle)))))
        (doseq [agent agents]
          (send agent detect-faces-agent! imageMatGray)
          (doall (map #(reset! all-detections-for-image (concat @all-detections-for-image (.toArray (:detections (deref %))))) agents))))
      )
    )
  (draw-bounding-boxes! imageMat))

(defn ^Mat capture-from-cam
  "Gets frame from cam and returns it as Mat."
  [^VideoCapture cam]
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
    (dosync (.load @lbph-face-recognizer recognizers-path))

    (.save @lbph-face-recognizer recognizers-path)
    )
  )

(defn update-lbph-recognizer
  ""
  [samples]
  (do
    (let [samples-count (.size @trainning-samples)
          mat (ref (Mat/zeros 1 samples-count CvType/CV_32SC1))]

      (.setTo (.row @mat 0) (Scalar. @label 0 0))
      (.update @lbph-face-recognizer samples @mat)
      (.setLabelInfo @lbph-face-recognizer @label @label-info)
      (send-off save-model-agent (fn [a rec path] (do (reset! training true) (.save rec path) (reset! training false)) a) @lbph-face-recognizer recognizers-path)
      (reset! trainning-samples [])
      )
    )
  )

(defn update-recognizer
  "docstring"
  [a matSamples]
  (do
    (toggle-collect-samples)
    (update-lbph-recognizer matSamples)
    (toggle-training))
  a
  )

(defn start-training [label-info-new label-to-train]
  (reset! label label-to-train)
  (reset! label-info label-info-new)
  (reset! traning-rectangle (Rect. 300 100 250 250))
  (let [detection-rect (first @all-detections-for-image)]
    (if (not (nil? detection-rect)) (reset! traning-rectangle detection-rect))
    )

  (reset! training true)
  (reset! collect-samples true)
  )

(defn parse-int [^String s]
  (Integer/parseInt (re-find #"\A-?\d+" s)))