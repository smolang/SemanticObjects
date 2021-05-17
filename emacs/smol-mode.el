;;; smol-mode.el --- Major mode for the Smol language -*- lexical-binding: t; -*-

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
      )
   'words)
  "List of SMOL keywords.")

(defconst smol-constants
  (regexp-opt
   '("True" "False" "null" "this" "Cont")
   'words)
  "List of SMOL constants.")

(defvar smol-font-lock-defaults
  (list
   (cons smol-keywords font-lock-keyword-face)
   (cons smol-constants font-lock-constant-face))
  "Font lock information for SMOL.")

(define-derived-mode smol-mode prog-mode "SMOL"
  "Major mode for editing SMOL files."
  :group 'smol
  (setq-local comment-use-syntax t
              comment-start "//"
              comment-end ""
              comment-start-skip "//+\\s-*")
  (setq font-lock-defaults (list 'smol-font-lock-defaults)))

;;;###autoload
(add-to-list 'auto-mode-alist '("\\.smol\\'" . smol-mode))
