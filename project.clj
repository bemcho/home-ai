(defproject home-ai "0.1.0-SNAPSHOT"
  :description "Opencv Face detect and trainning."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories  {"local" "file:repo" }
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [log4j/log4j "1.2.16" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
                 [org.slf4j/slf4j-log4j12 "1.6.4"]
                 [org.clojure/tools.logging "0.2.3"]
                 [ clj-logging-config "1.9.10"]
                 [opencv/opencv "3.1.0"]
                 [opencv/opencv-native "3.1.0"]
                 [clj-sockets "0.1.0"]
                 [lwjgl/lwjgl "2.9.3"]
                 ]

  :jvm-opts ["-Xmx8g" "-Djava.library.path=.\\native\\windows\\x86_64"]
  :injections [ (clojure.lang.RT/loadLibrary org.opencv.core.Core/NATIVE_LIBRARY_NAME)]
  :native-path "native"
  :main ^:skip-aot home-ai.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

