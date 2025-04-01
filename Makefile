out/debug:
	mkdir -p out/debug

out/release:
	mkdir -p out/release

debug-bin: out/debug
	scala-cli package . -f -o out/debug/sniper

watch:
	scala-cli package -w . -f -o out/debug/sniper

clean:
	rm -rf out
	scala-cli clean .

SUFFIX = $(shell bash -c "cat .build.scala | scala-cli run _ -M coursierName")
LTO_TYPE = $(shell bash -c "cat .build.scala | scala-cli run _ -M ltoFlag")

bin: out/release
	scala-cli package . -f -o out/release/sniper --native-mode release-fast $(LTO_TYPE)

platform-bin: out/release
	scala-cli package . -f -o out/release/sniper-$(SUFFIX) --native-mode release-fast $(LTO_TYPE)
