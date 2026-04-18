(ns sandbox-profile
  "SBPL profile generator.
   SBPL is S-expressions. Clojure is S-expressions.
   So we just read EDN, transform, and pr-str. No compiler needed."
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [clojure.walk :as walk]))

(def home (System/getProperty "user.home"))

;; ---------------------------------------------------------------------------
;; S-expression transforms
;; ---------------------------------------------------------------------------

(defn expand-home [s]
  (if (and (string? s) (str/starts-with? s "~/"))
    (str home (subs s 1))
    s))

(defn- resolve-value [worktree-path v]
  (cond
    (= v :worktree) worktree-path
    (string? v)     (expand-home v)
    :else           v))

(defn- resolve-rule [worktree-path rule]
  (walk/postwalk (partial resolve-value worktree-path) rule))

(defn- path-exists? [path]
  (fs/exists? (expand-home path)))

(defn- paths->rules [action operation paths]
  (->> paths
       (filter path-exists?)
       (mapv #(list action operation (list 'subpath (expand-home %))))))

;; ---------------------------------------------------------------------------
;; Profile assembly
;; ---------------------------------------------------------------------------

(defn build-rules
  "Read :sandbox template from config, merge with path settings,
   return flat list of SBPL rules."
  [{:keys [worktree-path no-network? config]}]
  (let [{:keys [sandbox deny-read-paths deny-write-paths allow-exec-paths]} config
        resolve (partial resolve-rule worktree-path)]
    (concat
     (map resolve (:header sandbox))
     (paths->rules 'deny 'file-read* deny-read-paths)
     (map resolve (:file-write sandbox))
     (paths->rules 'deny 'file-write* deny-write-paths)
     (map resolve (:process-exec sandbox))
     (paths->rules 'allow 'process-exec allow-exec-paths)
     ;; file-pattern: regex-based deny/allow (as raw strings, not S-exprs)
     (map #(str "(deny file-read* (regex #\"" % "\"))")
          (:deny-read-patterns config))
     (map #(str "(allow file-read* (regex #\"" % "\"))")
          (:allow-read-patterns config))
     (map #(str "(deny file-write* (regex #\"" % "\"))")
          (:deny-write-patterns config))
     (map #(str "(allow file-write* (regex #\"" % "\"))")
          (:allow-write-patterns config))
     (when no-network?
       (map resolve (:network-deny sandbox))))))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn generate-profile [opts]
  (->> (build-rules opts)
       (remove nil?)
       (map #(if (string? %) % (pr-str %)))
       (str/join "\n")))

(defn write-profile! [opts]
  (let [profile-str (generate-profile opts)
        tmp-file    (str "/tmp/cc-sandbox-" (System/currentTimeMillis) ".sb")]
    (spit tmp-file profile-str)
    tmp-file))
