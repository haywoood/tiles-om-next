(set-env!
  :source-paths #{"src"}
  :resource-paths #{"public"}
  :dependencies '[[org.clojure/clojure                 "1.9.0-alpha14"]
                  [org.clojure/clojurescript           "1.9.293"]
                  [org.omcljs/om                       "1.0.0-alpha47"]
                  [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                  [adzerk/boot-cljs                    "1.7.228-2"]
                  [org.clojure/tools.nrepl             "0.2.12"]
                  [pandeiro/boot-http                  "0.7.6"]
                  [adzerk/boot-reload                  "0.4.13"]
                  [adzerk/boot-cljs-repl               "0.3.3"]
                  [com.cemerick/piggieback             "0.2.1"]
                  [weasel                              "0.7.0"]
                  [org.clojure/tools.nrepl             "0.2.12"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]])

(deftask dev
  "Launch Immediate Feedback Development Environment"
  []
  (comp
   (serve :dir "target")
   (watch)
   (reload)
   (cljs-repl :nrepl-opts {:port 13337})
   (cljs)
   (target :dir #{"target"})))
