# programmer
STM32 programmer through bootloader ESP8266 - continuous delivery 

The bootloader consists of 2 parts :

- Sender : running on PC
-- Sender monitors if a new .bin file exists, if the timestamp changed it will send it to the bootloader driver directly
-- Sender generates the low level commands for the driver ( it was easier to debug Java on PC then C++ on MCU )
-- it can run in fully automated or manual mode
-- it provides a scrolling view on the UART output when not bootloading  
-- sends UDP packets to driver on pre-configured port/

- Bootloader driver : running on ESP8266
-- sends logs of his activity in syslog protocol to  a loghost, serial output cannot be used as reserved for bootloading
-- executes commands of RESET, BOOT0 level, BOOTLOADER commands
-- communicates through UDP
-- serialization is based on CBOR. cmd-id-bytes 
