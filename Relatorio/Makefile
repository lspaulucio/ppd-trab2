#  Makefile

PDFNAME = relatorio

TEMPLATEX = *.aux *.toc *.log *.dvi *.out *.gz *.bbl *.blg *.idx *.lot *.brf

all: compilaDoc

compilaDoc:
	@echo Generating pdf archive
	@pdflatex $(PDFNAME) > /dev/null
	@bibtex $(PDFNAME) > /dev/null
	@pdflatex $(PDFNAME) > /dev/null
	@pdflatex $(PDFNAME) > /dev/null
	@echo Done
	
clean:
	@echo Cleaning
	@rm $(TEMPLATEX) $(PDFNAME).pdf > /dev/null 2>&1
	@echo Done
