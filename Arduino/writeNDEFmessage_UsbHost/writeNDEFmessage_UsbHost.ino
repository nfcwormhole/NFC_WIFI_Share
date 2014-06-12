/* Copyright 2013-2014 Ten Wong, wangtengoo7@gmail.com  
*/

/*********************************************************
** sample: send data to USB Serial
***********************************************************/
#if ARDUINO >= 100
 #include "Arduino.h"
#else
 #include "WProgram.h"
#endif
#include <Wire.h>
#include <RF430CL330H_Shield.h>

#define IRQ   (3)
#define RESET (4)  
int led = 13;
RF430CL330H_Shield nfc(IRQ, RESET);

volatile byte into_fired = 0;
uint16_t flags = 0;

void setup(void) 
{
    Serial.begin(115200);    
    Serial.println("Hello!");
    pinMode(led, OUTPUT); 
    digitalWrite(led, HIGH);
    //RF430 init
    nfc.begin();
    
    //enable interrupt 1
    attachInterrupt(1, RF430_Interrupt, FALLING);
        
    Serial.println("Wait for read or write...");
}

void loop(void) 
{
    if(into_fired)
    {
        //clear control reg to disable RF
        nfc.Write_Register(CONTROL_REG, nfc.Read_Register(CONTROL_REG) & ~RF_ENABLE); 
        delay(750);
        
        //read the flag register to check if a read or write occurred
        flags = nfc.Read_Register(INT_FLAG_REG); 
        //Serial.print("INT_FLAG_REG = 0x");Serial.println(flags, HEX);

        //ACK the flags to clear
        nfc.Write_Register(INT_FLAG_REG, EOW_INT_FLAG + EOR_INT_FLAG); 
            
        if(flags & EOW_INT_FLAG)            //check if the tag was written
        {
            Serial.println("The tag was written!");
            printf_tag();
            digitalWrite(led, LOW);
            delay(2000);
            digitalWrite(led, HIGH);
        }
        else if(flags & EOR_INT_FLAG) //check if the tag was read
        {
            Serial.println("The tag was readed!");
            digitalWrite(led, LOW);
            delay(1000);
            digitalWrite(led, HIGH);
        }

        flags = 0;
        into_fired = 0; //we have serviced INT1

        //Configure INTO pin for active low and re-enable RF
        nfc.Write_Register(CONTROL_REG, nfc.Read_Register(CONTROL_REG) | RF_ENABLE); 
        
        //re-enable INTO
        attachInterrupt(1, RF430_Interrupt, FALLING);
    }

    delay(100);
}

/**
**  @brief  interrupt service
**/
void RF430_Interrupt()            
{
    into_fired = 1;
    detachInterrupt(1);//cancel interrupt
}

/**
**  @brief send data to USB Serial
**/
void printf_tag()
{
    uint16_t msg_length = 0;
    uint16_t tag_length = 0;
    
    /* get the NDEF Message Length */
    msg_length = nfc.Read_OneByte(0x001A) << 8 | nfc.Read_OneByte(0x001B);
     
    /* entire Tag length */
    tag_length = msg_length + 0x1C;
 
    byte buffer[tag_length];
    
    /* get entire tag data */
    nfc.Read_Continuous(0, buffer, tag_length);   
    #if 0 //wteng 2014-5-10
    Serial.println("RF430[]=0x");
    for (uint8_t i=0; i < tag_length; i++)
    {
        if (buffer[i] < 0x10)
            Serial.print("0"); 
        Serial.print(buffer[i], HEX);
        Serial.print(" "); 
        if (i%0x10 == 0x0F)
            Serial.println(); 
    }
    Serial.println(); 
    #endif
    
    // send to USB Serial
    Serial.write(0xFF); //start bit
    Serial.write(buffer, tag_length);
    Serial.write(0xFF); //stop bit
    
}

