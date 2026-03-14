#!/usr/bin/env bb

(ns hydra-sandbox.main
  (:require [clojure.edn :as edn]
            [sandbox-profile]))

(def config-path (str (System/getProperty "user.home") "/.config/hydra-sandbox/config.edn"))

(defn load-config []
  (edn/read-string (slurp config-path)))

(defn cmd-sandbox-profile [dir no-network?]
  (let [config       (load-config)
        profile-file (sandbox-profile/write-profile!
                      {:worktree-path dir
                       :no-network?   no-network?
                       :config        config})]
    (binding [*out* *err*]
      (println (str "🛡 hydra-sandbox: ON"
                    (when no-network? " [no-network]"))))
    (println profile-file)))

(defn usage []
  (println "Usage: bb main.bb sandbox-profile <dir> [--no-network]")
  (System/exit 1))

(let [args    *command-line-args*
      command (first args)]
  (case command
    "sandbox-profile" (let [dir         (second args)
                            no-network? (some #{"--no-network"} (rest (rest args)))]
                        (when-not dir
                          (println "Error: directory is required")
                          (System/exit 1))
                        (cmd-sandbox-profile dir (boolean no-network?)))
    (usage)))
