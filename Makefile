MAKEFLAGS += --silent

# Define SBT variable
SBT = sbt

# Default target
default: verilog

docs:
	@echo Generating docs
	mkdir -p $(BUILD_ROOT)/doc
	cd doc/user-guide && pdflatex -output-directory=$(BUILD_ROOT)/doc timer.tex | tee -a $(BUILD_ROOT)/doc/doc.rpt

# Start with a fresh directory
clean:
	@echo Cleaning...
	rm -rf generated target *anno.json ./*.rpt doc/*.rpt syn/*.rpt syn.log out test_run_dir target
	rm -rf project/project project/target
	# filter all files with bad extensions
	find . -type f -name "*.aux" -delete
	find . -type f -name "*.toc" -delete
	find . -type f -name "*.out" -delete
	find . -type f -name "*.log" -delete
	find . -type f -name "*.fdb_latexmk" -delete
	find . -type f -name "*.fls" -delete
	find . -type f -name "*.synctex.gz" -delete
	find . -type f -name "*.pdf" -delete

# Generate verilog from the Chisel code
verilog:
	@echo Generating Verilog...
	$(SBT) "runMain tech.rocksavage.Main verilog --mode print --module tech.rocksavage.chiselware.addrdecode.AddrDecode"

# Run the tests
test:
	@echo Running tests...
	@$(SBT) test

# Synthesize the design
synth: verilog
	@echo Synthesizing...
	@$(SBT) "runMain tech.rocksavage.Main synthesis --module tech.rocksavage.chiselware.addrdecode.AddrDecode --techlib synth/stdcells.lib"
