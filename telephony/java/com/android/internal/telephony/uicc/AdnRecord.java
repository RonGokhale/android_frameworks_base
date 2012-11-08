/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony.uicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.GsmAlphabet;

import java.util.Arrays;


/**
 *
 * Used to load or store ADNs (Abbreviated Dialing Numbers).
 *
 * {@hide}
 *
 */
public class AdnRecord implements Parcelable {
    static final String LOG_TAG = "GSM";

    //***** Instance Variables

    String alphaTag = null;
    String number = null;
    String email = null;
    String additionalNumber = null;
    int extRecord = 0xff;
    int efid;                   // or 0 if none
    int recordNumber;           // or 0 if none


    //***** Constants

    // In an ADN record, everything but the alpha identifier
    // is in a footer that's 14 bytes
    static final int FOOTER_SIZE_BYTES = 14;

    // Maximum size of the un-extended number field
    static final int MAX_NUMBER_SIZE_BYTES = 11;

    static final int EXT_RECORD_LENGTH_BYTES = 13;
    static final int EXT_RECORD_TYPE_ADDITIONAL_DATA = 2;
    static final int EXT_RECORD_TYPE_MASK = 3;
    static final int MAX_EXT_CALLED_PARTY_LENGTH = 0xa;

    // ADN offset
    static final int ADN_BCD_NUMBER_LENGTH = 0;
    static final int ADN_TON_AND_NPI = 1;
    static final int ADN_DIALING_NUMBER_START = 2;
    static final int ADN_DIALING_NUMBER_END = 11;
    static final int ADN_CAPABILITY_ID = 12;
    static final int ADN_EXTENSION_ID = 13;

   /*modified for number2 begin*/
   static final int FOOTER_SIZE_BYTES_FOR_USIM_ANR = 15;

   static final int EMAIL_SIZE_BYTES = 40;

    static final int ANR_ADITION_REC_IDENTI = 0;
    static final int ANR_BCD_NUMBER_LENGTH = 1;
    static final int ANR_TON_AND_NPI = 2;
    static final int ANR_DIALING_NUMBER_START = 3;
    static final int ANR_DIALING_NUMBER_END = 12;
    static final int ANR_CAPABILITY_ID = 13;
    static final int ANR_EXTENSION_ID = 14;
   /*modified for number2 end*/
    //***** Static Methods

    public static final Parcelable.Creator<AdnRecord> CREATOR
            = new Parcelable.Creator<AdnRecord>() {
        public AdnRecord createFromParcel(Parcel source) {
            int efid;
            int recordNumber;
            String alphaTag;
            String number;
            String email;
            String additionalNumber;
            efid = source.readInt();
            recordNumber = source.readInt();
            alphaTag = source.readString();
            number = source.readString();
            email = source.readString();
            additionalNumber = source.readString();

            return new AdnRecord(efid, recordNumber, alphaTag, number, email, additionalNumber);
        }

        public AdnRecord[] newArray(int size) {
            return new AdnRecord[size];
        }
    };


    //***** Constructor
    public AdnRecord (byte[] record) {
        this(0, 0, record);
    }

    public AdnRecord (int efid, int recordNumber, byte[] record) {
        this.efid = efid;
        this.recordNumber = recordNumber;
        parseRecord(record);
    }

    public AdnRecord (String alphaTag, String number) {
        this(0, 0, alphaTag, number);
    }

    public AdnRecord (String alphaTag, String number, String email) {
        this(0, 0, alphaTag, number, email);
    }

    public AdnRecord (String alphaTag, String number, String email, String additionalNumber) {
        this(0, 0, alphaTag, number, email, additionalNumber);
    }
    public AdnRecord (int efid, int recordNumber, String alphaTag, String number, String email) {
        this.efid = efid;
        this.recordNumber = recordNumber;
        this.alphaTag = alphaTag;
        this.number = number;
        this.email = email;
        this.additionalNumber = null;
    }

    public AdnRecord (int efid, int recordNumber, String alphaTag, String number, String email, String additionalNumber) {
        this.efid = efid;
        this.recordNumber = recordNumber;
        this.alphaTag = alphaTag;
        this.number = number;
        this.email = email;
        this.additionalNumber = additionalNumber;
    }
    public AdnRecord(int efid, int recordNumber, String alphaTag, String number) {
        this.efid = efid;
        this.recordNumber = recordNumber;
        this.alphaTag = alphaTag;
        this.number = number;
        this.email = null;
        this.additionalNumber = null;
    }

    //***** Instance Methods

    public String getAlphaTag() {
        return alphaTag;
    }

    public String getNumber() {
        return number;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAdditionalNumber() {
        return additionalNumber;
    }
    public void setAdditionalNumber(String additionalNumber) {
        this.additionalNumber = additionalNumber;
    }
    public String toString() {
        return "ADN Record '" + alphaTag + "' '" + number + " "
            + email + " " + additionalNumber + "'" ;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(alphaTag) && TextUtils.isEmpty(number)
            && email == null && additionalNumber == null;
    }

    public boolean hasExtendedRecord() {
        return extRecord != 0 && extRecord != 0xff;
    }

    /** Helper function for {@link #isEqual}. */
    private static boolean stringCompareNullEqualsEmpty(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null) {
            s1 = "";
        }
        if (s2 == null) {
            s2 = "";
        }
        return (s1.equals(s2));
    }

    public boolean isEqual(AdnRecord adn) {
        return ( stringCompareNullEqualsEmpty(alphaTag, adn.alphaTag) &&
                stringCompareNullEqualsEmpty(number, adn.number) &&
                stringCompareNullEqualsEmpty(email, adn.email) &&
                stringCompareNullEqualsEmpty(additionalNumber, adn.additionalNumber)) ;
    }
    //***** Parcelable Implementation

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(efid);
        dest.writeInt(recordNumber);
        dest.writeString(alphaTag);
        dest.writeString(number);
        dest.writeString(email);
        dest.writeString(additionalNumber);
    }

    /**
     * Build adn hex byte array based on record size
     * The format of byte array is defined in 51.011 10.5.1
     *
     * @param recordSize is the size X of EF record
     * @return hex byte[recordSize] to be written to EF record
     *          return null for wrong format of dialing number or tag
     */
    public byte[] buildAdnString(int recordSize) {
        byte[] bcdNumber;
        byte[] byteTag;
        byte[] adnString;
        int footerOffset = recordSize - FOOTER_SIZE_BYTES;

        // create an empty record
        adnString = new byte[recordSize];
        for (int i = 0; i < recordSize; i++) {
            adnString[i] = (byte) 0xFF;
        }

        if (TextUtils.isEmpty(number)) {
            Log.w(LOG_TAG, "[buildAdnString] Empty dialing number");
            return adnString;   // return the empty record (for delete)
        } else if (number.length()
                > (ADN_DIALING_NUMBER_END - ADN_DIALING_NUMBER_START + 1) * 2) {
            Log.w(LOG_TAG,
                    "[buildAdnString] Max length of dialing number is 20");
            return null;
        } else if (alphaTag != null && alphaTag.length() > footerOffset) {
            Log.w(LOG_TAG,
                    "[buildAdnString] Max length of tag is " + footerOffset);
            return null;
        } else {
            bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(number);

            System.arraycopy(bcdNumber, 0, adnString,
                    footerOffset + ADN_TON_AND_NPI, bcdNumber.length);

            adnString[footerOffset + ADN_BCD_NUMBER_LENGTH]
                    = (byte) (bcdNumber.length);
            adnString[footerOffset + ADN_CAPABILITY_ID]
                    = (byte) 0xFF; // Capability Id
            adnString[footerOffset + ADN_EXTENSION_ID]
                    = (byte) 0xFF; // Extension Record Id

            if (!TextUtils.isEmpty(alphaTag)) {
                byteTag = GsmAlphabet.stringToGsm8BitPacked(alphaTag);
                System.arraycopy(byteTag, 0, adnString, 0, byteTag.length);
            }

            return adnString;
        }
    }
   /*modified for number2 begin*/
    public byte[] buildAnrString(int index) {
        byte[] bcdNumber;
        byte[] anrString;
        String aditionalNum = null;

        // create an empty record
        anrString = new byte[FOOTER_SIZE_BYTES_FOR_USIM_ANR];
        for (int i = 0; i < FOOTER_SIZE_BYTES_FOR_USIM_ANR; i++) {
            anrString[i] = (byte) 0xFF;
        }

        if(TextUtils.isEmpty(additionalNumber)){
            return null;
        }else{
            aditionalNum = additionalNumber;
        }

        if (TextUtils.isEmpty(aditionalNum)) {
            Log.w(LOG_TAG, "[buildAnrString] Empty dialing aditional number");
            //return null;   // return the empty record (for delete)
	    /*2012-2-16-RIL-zhouyi-return 'FF' data to delete ANR record-Start*/
	    return anrString;
	    /*2012-2-16-RIL-zhouyi-return 'FF' data to delete ANR record-End*/
        } else if (aditionalNum.length() > (ANR_DIALING_NUMBER_END - ANR_DIALING_NUMBER_START + 1) * 2) {
            Log.w(LOG_TAG, "[buildAnrString] Max length of dialing aditional number is 20");
            return null;
        }  else {
            bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(aditionalNum);

            System.arraycopy(bcdNumber, 0, anrString, ANR_TON_AND_NPI, bcdNumber.length);

            anrString[ANR_ADITION_REC_IDENTI] = (byte) (index);
            anrString[ANR_BCD_NUMBER_LENGTH] = (byte) (bcdNumber.length);
            anrString[ANR_CAPABILITY_ID] = (byte) 0xFF; // Capability Id
            anrString[ANR_EXTENSION_ID] = (byte) 0xFF; // Extension Record Id

            return anrString;
        }
    }
    public byte[] encodeEmails(){
        Log.d(LOG_TAG, "encodeEmails ---IN!!!");
        byte[] emailByte;
        byte[] emailByteArr;
        emailByteArr = new byte[EMAIL_SIZE_BYTES];
        for (int i = 0; i < EMAIL_SIZE_BYTES; i++) {
            emailByteArr[i] = (byte) 0xFF;
        }
       
        if(TextUtils.isEmpty(email)) return null;
        //int size =  emails.length;
        //Log.d(LOG_TAG, "encodeEmails --emails.length = " + emails.length);
        //if(size > 0){
          emailByte = IccUtils.hexStringToBytes(toHexString(email));
          System.arraycopy(emailByte, 0, emailByteArr, 0, emailByte.length);
          return emailByteArr;
        //}
        //return null;
    }
   public String toHexString(String s)  
   {  
       String str="";  
       for (int i=0;i<s.length();i++)  
       {  
           int ch = (int)s.charAt(i);  
           String s4 = Integer.toHexString(ch);  
           str = str + s4;
        }  
        return  str;
    }

    /**
     * See TS 51.011 10.5.10
     */
    public void
    appendExtRecord (byte[] extRecord) {
        try {
            if (extRecord.length != EXT_RECORD_LENGTH_BYTES) {
                return;
            }

            if ((extRecord[0] & EXT_RECORD_TYPE_MASK)
                    != EXT_RECORD_TYPE_ADDITIONAL_DATA) {
                return;
            }

            if ((0xff & extRecord[1]) > MAX_EXT_CALLED_PARTY_LENGTH) {
                // invalid or empty record
                return;
            }

            number += PhoneNumberUtils.calledPartyBCDFragmentToString(
                                        extRecord, 2, 0xff & extRecord[1]);

            // We don't support ext record chaining.

        } catch (RuntimeException ex) {
            Log.w(LOG_TAG, "Error parsing AdnRecord ext record", ex);
        }
    }

    //***** Private Methods

    /**
     * alphaTag and number are set to null on invalid format
     */
    private void
    parseRecord(byte[] record) {
        try {
        	if(record == null || record.length == 0)
        	{
        		Log.w(LOG_TAG, "Error parsing AdnRecord ---- record == null || record.length == 0 ");
        		alphaTag = null;
        		number = null;
				email = null;
				additionalNumber = null;
        		return;
        	}
        	
            alphaTag = IccUtils.adnStringFieldToString(
                            record, 0, record.length - FOOTER_SIZE_BYTES);

            int footerOffset = record.length - FOOTER_SIZE_BYTES;

            int numberLength = 0xff & record[footerOffset];

            if (numberLength > MAX_NUMBER_SIZE_BYTES) {
                // Invalid number length
                number = "";
                return;
            }

            // Please note 51.011 10.5.1:
            //
            // "If the Dialling Number/SSC String does not contain
            // a dialling number, e.g. a control string deactivating
            // a service, the TON/NPI byte shall be set to 'FF' by
            // the ME (see note 2)."

            number = PhoneNumberUtils.calledPartyBCDToString(
                            record, footerOffset + 1, numberLength);


            extRecord = 0xff & record[record.length - 1];

            email = null;
            additionalNumber = null;

        } catch (RuntimeException ex) {
            Log.w(LOG_TAG, "Error parsing AdnRecord", ex);
            number = "";
            alphaTag = "";
            email = null;
            additionalNumber = null;
        }
    }
}
