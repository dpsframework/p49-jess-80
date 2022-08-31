; -*- jess -*-

;; Jess standard script library
;; You can add functions to this file and they will be loaded in whenever
;; Jess starts up
;; (C) 2004 Sandia Corporation


(deffunction show-deftemplates ()
  "Print deftemplates"
  (bind ?__e (call (engine) listDeftemplates))
  (while (call ?__e hasNext)
      (printout t crlf
          (call (new jess.PrettyPrinter (call ?__e next)) toString) crlf)))

(deffunction show-jess-listeners ()
  "Print JessListeners"
  (bind ?__e (call (engine) listJessListeners))
  (while (call ?__e hasNext)
    (printout t crlf (call (call ?__e next) toString) crlf)))

(deffunction show-deffacts ()
  "Print deffacts"
  (bind ?__e (call (engine) listDeffacts))
  (while (call ?__e hasNext)
    (printout t crlf (call (new jess.PrettyPrinter (call ?__e next)) toString)
               crlf)))

(deffunction ppdefrule (?__rule)
  "Pretty-print a Jess rule, or all rules and queries"
  (if (eq ?__rule *) then
    (bind ?sb (new StringBuffer))
    (bind ?it ((engine) listDefrules))
    (while (?it hasNext)
      (?sb append (new jess.PrettyPrinter (?it next)))
      (?sb append "
"))
    (?sb toString)
    else
    (bind ?__defrule (call (engine) findDefrule ?__rule))
    (if (neq ?__defrule nil) then
      (call (new jess.PrettyPrinter ?__defrule) toString)
      else
      (str-cat "No such rule: " ?__rule))))


(deffunction ppdeffacts (?__facts)
  "Pretty-print a deffacts"
  (bind ?__deffacts (call (engine) findDeffacts ?__facts))
  (if (neq ?__deffacts nil) then
    (call (new jess.PrettyPrinter ?__deffacts) toString)
    else
    (str-cat "No such deffacts: " ?__facts)))

(deffunction ppdefquery (?__query)
  "Pretty-print a Jess query, or all rules and queries"
  (ppdefrule ?__query))

(deffunction ppdeffunction (?__function)
  "Pretty-print a Jess deffunction"
  (bind ?__deffunction (call (engine) findUserfunction ?__function))
  (if (neq ?__deffunction nil) then
    (if (instanceof ?__deffunction jess.Visitable) then
      (call (new jess.PrettyPrinter ?__deffunction) toString)
      else
      (str-cat "Not a deffunction: " ?__function))
    else
    (str-cat "No such deffunction: " ?__function)))

(deffunction ppdefglobal (?__global)
  "Pretty-print a Jess global"
  (bind ?__defglobal (call (engine) findDefglobal ?__global))
  (if (neq ?__defglobal nil) then
    (call (new jess.PrettyPrinter ?__defglobal) toString)
    else
    (str-cat "No such defglobal: " ?__global)))

(deffunction ppdeftemplate (?__template)
  "Pretty-print a Jess template"
  (bind ?__deftemplate (call (engine) findDeftemplate ?__template))
  (if (neq ?__deftemplate nil) then
    (call (new jess.PrettyPrinter ?__deftemplate) toString)
    else
    (str-cat "No such deftemplate: " ?__template)))

(deffunction run-until-halt ()
  "Run until halt is called."
  (call (engine) runUntilHalt))
  
 ;; Load viewfunctions, if it exists
 (try
 	(load-package jess.awt.ViewFunctions)
  catch ?ERROR
    ;; Don't care
  )

  

;; Create Jess functions out of the static members of all of these classes
(import java.lang.Boolean)
(import java.lang.Byte)
(import java.lang.Character)
(import java.lang.Class)
(import java.lang.Double)
(import java.lang.Float)
(import java.lang.Integer)
(import java.lang.Long)
(import java.lang.Math)
(import java.lang.Runtime)
(import java.lang.Short)
(import java.lang.String)
(import java.lang.System)
(import java.lang.Thread)
(import java.lang.Void)
(import jess.JessEvent)
(import jess.RU)
