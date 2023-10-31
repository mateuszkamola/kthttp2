Implementing simple HTTP2 server based on the official RFC https://www.rfc-editor.org/rfc/rfc7540.html
Currently only HTTPS version with ALPN negotiation is going to be supported.

Implemented:
- simple SSL server and accepting sockets
- setting h2 as application protocol via ALPN
- receiving and sending data on connected sockets
- sending and receiving preface
- parsing Frame data

How to run
- create self-signed key and certificate (up to and including step 4 - https://www.baeldung.com/openssl-self-signed-cert)
    - use password 'secret'
- bundle key and crt into pfx (pkcs12) file - step 7.2 https://www.baeldung.com/openssl-self-signed-cert
    - password 'secret'
- copy pkcs file into 'app' folder and name it domain.p12
- $ ./gradlew run

TODO:
- parsing headers frame
    - implement compression mechanism https://www.rfc-editor.org/rfc/rfc7541
- parse settings frame
- parse data frame
- some kind of logging mechanism
- refactor net/http2/Server.kt (maybe FrameType should be an enum, split into more classes, different frame handlers etc.)
- add support for config file to handle common things like port number, listen interface, pkcs12 file, password etc.
