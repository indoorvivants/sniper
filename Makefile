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

out/flags/lto:
	mkdir -p out/flags
	cat .build.scala | scala-cli run _ -M ltoFlag > out/flags/lto

out/flags/platform:
	mkdir -p out/flags
	cat .build.scala | scala-cli run _ -M coursierName > out/flags/platform

bin: out/release out/flags/lto
	scala-cli package . -f -o out/release/sniper --native-mode release-fast $(LTO_TYPE)

platform-bin: out/release out/flags/platform out/flags/lto
	scala-cli package . -f -o out/release/sniper-$$(cat out/flags/platform) --native-mode release-fast $$(cat out/flags/lto)

install: bin
	echo "Installing ./out/release/sniper into /usr/local/bin/sniper"
	echo "This command will be run with sudo, so your password may be required"
	sudo install -m 755 out/release/sniper /usr/local/bin/sniper

publish-snapshot:
	scala-cli config publish.credentials oss.sonatype.org env:SONATYPE_USERNAME env:SONATYPE_PASSWORD
	scala-cli publish *.scala --signer none

publish:
	scala-cli config publish.credentials oss.sonatype.org env:SONATYPE_USERNAME env:SONATYPE_PASSWORD
	./.github/workflows/import-gpg.sh
	scala-cli publish *.scala --signer gpg --gpg-key 9D8EF0F74E5D78A3
