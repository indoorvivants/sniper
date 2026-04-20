test-bootstrap:
	echo "FROM ubuntu\nCOPY ./out/debug/sniper /usr/bin/\nRUN sniper print-config" | docker build . -f -
