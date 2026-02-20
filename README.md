
# cardutil-java

**cardutil-java** is a Java port of the [cardutil](https://github.com/adelosa/cardutil) Python package for working with payment card systems, including command-line tools for working with Mastercard IPM files.

![Java CI](https://github.com/charisad/cardutil.java/actions/workflows/maven.yml/badge.svg)
![JitPack](https://jitpack.io/v/charisad/cardutil.java.svg)

## Features
* **ISO8583 Message Parsing**: Parse and pack ISO8583 messages.
* **Mastercard IPM File Handling**: Read, write, and convert Mastercard IPM files (including 1014 blocking support).
* **CLI Tools**: Convert between IPM and CSV formats.
* **Cryptography Utilities**: Check digit calculator, Pin Block generator, Visa PVV calculator.
* **Zero Dependencies**: Core library relies only on standard Java libraries (except for CLI which uses `commons-csv`).

## Installation

### Maven (via JitPack)
Add the JitPack repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add the dependency:

```xml
<dependency>
    <groupId>com.github.charisad</groupId>
    <artifactId>cardutil.java</artifactId>
    <version>Tag</version>
</dependency>
```

## Usage

### Java API

#### ISO8583 Parsing
```java
import com.charisad.cardutil.Iso8583;
import java.util.Map;

byte[] messageBytes = ...;
Map<String, Object> data = Iso8583.unpack(messageBytes, null);
System.out.println("MTI: " + data.get("MTI"));
System.out.println("PAN: " + data.get("DE2"));
```

#### IPM File Reading
```java
import com.charisad.cardutil.MciIpm;
import java.nio.file.Files;
import java.nio.file.Paths;

try (InputStream is = Files.newInputStream(Paths.get("incoming.ipm"));
     MciIpm.IpmReader reader = new MciIpm.IpmReader(is, true)) { // true for 1014 blocking
    for (Map<String, Object> record : reader) {
        System.out.println(record);
    }
}
```

### Command Line Interface (CLI)

The library includes a CLI for common tasks.

#### Convert IPM to CSV
```bash
mvn exec:java -Dexec.mainClass="com.charisad.cardutil.Cli" -Dexec.args="ipm2csv input.ipm -o output.csv"
```

#### Convert CSV to IPM
```bash
mvn exec:java -Dexec.mainClass="com.charisad.cardutil.Cli" -Dexec.args="csv2ipm input.csv -o output.ipm"
```

## Acknowledgements

This project is a direct port of the Python [cardutil](https://github.com/adelosa/cardutil) library by Anthony Delosa.
*   Original Author: [Anthony Delosa](https://github.com/adelosa)
*   Original License: MIT

Ported and maintained by [charisad](https://github.com/charisad).

## License
MIT
