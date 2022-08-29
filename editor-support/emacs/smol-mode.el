;;; smol-mode.el --- Major mode for the Smol language -*- lexical-binding: t; -*-
;; Copyright (C) 2021-2022  Rudolf Schlatte

;; Author: Rudi Schlatte <rudi@constantly.at>
;; URL: https://github.com/Edkamb/SemanticObjects/tree/master/emacs
;; Version: 1.0
;; Package-Requires: ((emacs "27.1"))
;; Keywords: languages

;; This program is free software; you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, version 3.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

;;; Commentary:

;; A major mode for editing files for the SMOL language.
;;

;;; Customization
(defgroup smol nil
  "Major mode for editing files in the SMOL language."
  :group 'languages)

(defcustom smol-jar-file "build/libs/smol-0.2-all.jar"
  "The jar file containing the SMOL interpreter."
  :type '(file :must-match t)
  :group 'smol
  :risky t)


(defvar smol-mode-syntax-table (copy-syntax-table)
  "Syntax table for `smol-mode'.")
(modify-syntax-entry ?/   ". 124" smol-mode-syntax-table)
(modify-syntax-entry ?*   ". 23b" smol-mode-syntax-table)
(modify-syntax-entry ?\n  ">"     smol-mode-syntax-table)
(modify-syntax-entry ?\^m ">"     smol-mode-syntax-table)

(defconst smol-keywords
  (regexp-opt
   ;; taken from `While.g4'
   '("skip" "return" "if" "then" "new" "else" "while" "do" "print" "end"
     "access" "construct" "derive" "simulate" "tick" "breakpoint"
     "super" "destroy" "abstract"
     "class" "extends" "rule" "override" "main"
     "private" "protected" "inferprivate" "inferprotected"
     "port"
     "in" "out"
      )
   'words)
  "List of SMOL keywords.")

(defconst smol-constants
  (regexp-opt
   '("True" "False" "null" "this" "unit")
   'words)
  "List of SMOL constants.")

(defconst smol-types
  ;; See end of `Types.kt'
  (regexp-opt
   '("Int" "Boolean" "String" "Double" "Object" "Null" "Unit" "Cont")
   'words)
  "List of SMOL type names.")

(defvar smol-font-lock-defaults
  (list
   (cons smol-keywords font-lock-keyword-face)
   (cons smol-constants font-lock-constant-face)
   (cons smol-types font-lock-type-face))
  "Font lock information for SMOL.")

(define-derived-mode smol-mode prog-mode "SMOL"
  "Major mode for editing SMOL files."
  :group 'smol
  (setq-local comment-use-syntax t
              comment-start "//"
              comment-end ""
              comment-start-skip "//+\\s-*")
  (setq font-lock-defaults (list 'smol-font-lock-defaults))
  (define-key smol-mode-map (kbd "C-c C-c")
    'smol-eval-current-file))

;;;###autoload
(add-to-list 'auto-mode-alist '("\\.smol\\'" . smol-mode))

;; The REPL

(defvar smol--inferior-smol-buffer nil
  "The buffer running the SMOL REPL.")

(define-derived-mode inferior-smol-mode
  comint-mode "inferior-smol"
  "Major mode for running the SMOL REPL."
  (setq comint-process-echoes t))

;;;###autoload
(defun run-smol ()
  "Start an inferior SMOL REPL.
If a smol repl is already running, switch to its buffer."
  (interactive)
  (if (comint-check-proc smol--inferior-smol-buffer)
      (pop-to-buffer smol--inferior-smol-buffer)
    (when (buffer-live-p smol--inferior-smol-buffer)
      (kill-buffer smol--inferior-smol-buffer))
    (setq smol--inferior-smol-buffer
          (make-comint "SMOL" "java" nil "-jar" smol-jar-file))
    (pop-to-buffer smol--inferior-smol-buffer)
    (inferior-smol-mode)
    (ansi-color-for-comint-mode-on)
    ;; (set (make-local-variable 'compilation-error-regexp-alist)
    ;;      smol-compilation-regexp-alist)
    ;; (compilation-shell-minor-mode 1)
    (set-process-query-on-exit-flag
     (get-buffer-process smol--inferior-smol-buffer)
     nil)))

;;;###autoload
(defun smol-eval-current-file (arg)
  "Start or switch to a SMOL REPL and evaluate the current file.
When invoked with the `C-u' prefix (i.e., when ARG is set), do
not evaluate the file in SMOL; otherwise, save the buffer when
modified."
  (interactive "P")
  (if arg
      (run-smol)
    (save-buffer)
    (let ((filename (buffer-file-name (current-buffer))))
      (run-smol)
      (sit-for 0.1)
      (comint-send-string smol--inferior-smol-buffer
                          (format "reada %s\n" filename)))))

(provide 'smol-mode)
;;; smol-mode.el ends here
