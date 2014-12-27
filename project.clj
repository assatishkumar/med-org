(defproject med-org "0.1.0"
  :description "This is a command line utility which helps you in organizing your media files chronologically."
  :url "https://github.com/assatishkumar/med-org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [io.joshmiller/exif-processor "0.1.1"]
                 [me.raynes/fs "1.4.6"]
                 [clj-time "0.8.0"]]
  :main ^:skip-aot med-org.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
