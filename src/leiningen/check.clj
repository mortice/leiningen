(ns leiningen.check
  "Check syntax and warn on reflection."
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.logger :as log]
            [bultitude.core :as b]
            [clojure.java.io :as io]))

(defn check
  "Check syntax and warn on reflection."
  ([project]
     (let [source-files (map io/file (:source-paths project))
           nses (b/namespaces-on-classpath :classpath source-files)
           action `(let [failures# (atom 0)]
                     (doseq [ns# '~nses]
                       ;; load will add the .clj, so can't use ns/path-for.
                       (let [ns-file# (-> (str ns#)
                                          (.replace \- \_)
                                          (.replace \. \/))]
                         (println "Compiling namespace" ns#)
                         (try
                           (binding [*warn-on-reflection* true]
                             (load ns-file#))
                           (catch ExceptionInInitializerError e#
                             (swap! failures# inc)
                             (.printStackTrace e#)))))
                     (System/exit @failures#))]
       (try (eval/eval-in-project project action)
            (catch clojure.lang.ExceptionInfo e
              (log/abort "Failed."))))))
