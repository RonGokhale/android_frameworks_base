/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.internal.telephony.gsm;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import android.telephony.PhoneNumberUtils;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class implements reading and parsing USIM records.
 * Refer to Spec 3GPP TS 31.102 for more details.
 *
 * {@hide}
 */
public class UsimPhoneBookManager extends Handler implements IccConstants {
    private static final String LOG_TAG = "GSM";
    private static final boolean DBG = true;
    
    public static int tempNumRec = 0;
    
    private PbrFile mPbrFile;
    private Boolean mIsPbrPresent;
    private IccFileHandler mFh;
    private AdnRecordCache mAdnCache;
    private Object mLock = new Object();
    private ArrayList<AdnRecord> mPhoneBookRecords;
    private boolean mEmailPresentInIap = false;
    private boolean mAnrPresentInIap = false;
    private boolean mSnePresentInIap = false;
    private int mEmailTagNumberInIap = 0;
    private int mAnrTagNumberInIap   = 0;
	private int availableEmailSlotNumber = 0;
	private boolean EmailSlotFound = false;
	private ArrayList<Integer> existEmailRecord = new ArrayList<Integer>();
    private ArrayList<byte[]> mIapFileRecord;
    private ArrayList<byte[]> mEmailFileRecord;
 
    private ArrayList<AnrFile> mAnrFile;
    private Map<Integer, ArrayList<String>> mEmailsForAdnRec;
    private boolean mRefreshCache = false;

    private static final int EVENT_PBR_LOAD_DONE = 1;
    private static final int EVENT_USIM_ADN_LOAD_DONE = 2;
    private static final int EVENT_IAP_LOAD_DONE = 3;
    private static final int EVENT_EMAIL_LOAD_DONE = 4;

    private static final int EVENT_ANR_LOAD_DONE        = 6;

    private static final int USIM_TYPE1_TAG   = 0xA8;
    private static final int USIM_TYPE2_TAG   = 0xA9;
    private static final int USIM_TYPE3_TAG   = 0xAA;
    private static final int USIM_EFADN_TAG   = 0xC0;
    private static final int USIM_EFIAP_TAG   = 0xC1;
    private static final int USIM_EFEXT1_TAG  = 0xC2;
    private static final int USIM_EFSNE_TAG   = 0xC3;
    private static final int USIM_EFANR_TAG   = 0xC4;
    private static final int USIM_EFPBC_TAG   = 0xC5;
    private static final int USIM_EFGRP_TAG   = 0xC6;
    private static final int USIM_EFAAS_TAG   = 0xC7;
    private static final int USIM_EFGSD_TAG   = 0xC8;
    private static final int USIM_EFUID_TAG   = 0xC9;
    private static final int USIM_EFEMAIL_TAG = 0xCA;
    private static final int USIM_EFCCP1_TAG  = 0xCB;

    public UsimPhoneBookManager(IccFileHandler fh, AdnRecordCache cache) {
        mFh = fh;
        mPhoneBookRecords = new ArrayList<AdnRecord>();
        mPbrFile = null;
        // We assume its present, after the first read this is updated.
        // So we don't have to read from UICC if its not present on subsequent reads.
        mIsPbrPresent = true;
        mAdnCache = cache;
    }

    public void reset() {
        mPhoneBookRecords.clear();
        mIapFileRecord = null;
        mEmailFileRecord = null;
        mPbrFile = null;
        mIsPbrPresent = true;
        mRefreshCache = false;
    }

    public ArrayList<AdnRecord> loadEfFilesFromUsim() {
        synchronized (mLock) {
            if (!mPhoneBookRecords.isEmpty()) {
                if (mRefreshCache) {
                    mRefreshCache = false;
                    refreshCache();
                }
                return mPhoneBookRecords;
            }

            if (!mIsPbrPresent) return null;

            // Check if the PBR file is present in the cache, if not read it
            // from the USIM.
            if (mPbrFile == null) {
                readPbrFileAndWait();
            }

            if (mPbrFile == null) return null;

            int numRecs = mPbrFile.mFileIds.size();
            
            
            Log.e(LOG_TAG, "loadEfFilesFromUsim numRecs: "+numRecs);
            for (int i = 0; i < numRecs; i++) {
/*2012-02-28-RIL-zhouyi-supply ADN file number to AdnRecordLoader to construct ADN record-Start*/
            	tempNumRec = i;	//load for two times, and the results construction in AdnRecordLoader will use this to decide index from 0 or from 250
/*2012-02-28-RIL-zhouyi-supply ADN file number to AdnRecordLoader to construct ADN record-End*/
                readAdnFileAndWait(i);
                readEmailFileAndWait(i);
                readAnrFileAndWait(i);
                /*2012-03-01-RIL-zhouyi-email and ANR is not needed any more-End*/
            }
            // All EF files are loaded, post the response.
        }
        return mPhoneBookRecords;
    }

    private void refreshCache() {
        if (mPbrFile == null) return;
        mPhoneBookRecords.clear();

        int numRecs = mPbrFile.mFileIds.size();
        for (int i = 0; i < numRecs; i++) {
            readAdnFileAndWait(i);
        }
    }

    public void invalidateCache() {
        mRefreshCache = true;
    }

    private void readPbrFileAndWait() {
        mFh.loadEFLinearFixedAll(EF_PBR, obtainMessage(EVENT_PBR_LOAD_DONE));
        try {
            mLock.wait();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
        }
    }

    private void readEmailFileAndWait(int recNum) {
        Map <Integer,Integer> fileIds;
        fileIds = mPbrFile.mFileIds.get(recNum);
        if (fileIds == null) return;

        Log.e(LOG_TAG, "readEmailFileAndWait");

        if (fileIds.containsKey(USIM_EFEMAIL_TAG)) {
            int efid = fileIds.get(USIM_EFEMAIL_TAG);
            // Check if the EFEmail is a Type 1 file or a type 2 file.
            // If mEmailPresentInIap is true, its a type 2 file.
            // So we read the IAP file and then read the email records.
            // instead of reading directly.

        if (mEmailPresentInIap && mIapFileRecord == null) {
                readIapFileAndWait(fileIds.get(USIM_EFIAP_TAG));
                if (mIapFileRecord == null) {
                    Log.e(LOG_TAG, "Error: IAP file is empty");
                    return;
                }
            }
            // Read the EFEmail file.
            Log.e(LOG_TAG, "readEmailFileAndWait EVENT_EMAIL_LOAD_DONE");
            mFh.loadEFLinearFixedAll(fileIds.get(USIM_EFEMAIL_TAG),
                    obtainMessage(EVENT_EMAIL_LOAD_DONE));
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Interrupted Exception in readEmailFileAndWait");
            }

            if (mEmailFileRecord == null) {
                Log.e(LOG_TAG, "Error: Email file is empty");
                return;
            }
            updatePhoneAdnRecord();
        }
    }

    /**
     * Read EF_ANR record. During reading, object is locked.
     * <br>After reading completion, call handleMessage() method with EVENT_ANR_LOAD_DONE. 
     * @param recNum.
     */
    private void readAnrFileAndWait(int recNum) {
        Map <Integer,Integer> fileIds;
        fileIds = mPbrFile.mFileIds.get(recNum);
        if (fileIds == null) return;

        if (fileIds.containsKey(USIM_EFANR_TAG)) {
            int efid = fileIds.get(USIM_EFANR_TAG);
            // If EF_ANR is Type 2 EF, read EF_IAP first.
            if (DBG) {
                if (mAnrPresentInIap) {
                    log("EF_ANR in EF_IAP. EF_ANR is Type 2 EF.");
                } else {
                    log("EF_ANR is Type 1 EF.");
                }
            }

            if (mAnrPresentInIap && mIapFileRecord == null) {
                readIapFileAndWait(fileIds.get(USIM_EFIAP_TAG));
                if (mIapFileRecord == null) {
                    Log.e(LOG_TAG, "Error: IAP file is empty");
                    return;
                }
            }

            //readEfFileAndWait(recNum, USIM_EFANR_TAG, EVENT_ANR_LOAD_DONE);
            mFh.loadEFLinearFixedAll(fileIds.get(USIM_EFANR_TAG),
                        obtainMessage(EVENT_ANR_LOAD_DONE));
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Interrupted Exception in readAnrFileAndWait");
            }

            if ((mAnrFile == null)/*||((mAnrFileRecord == null))*/) {
                Log.e(LOG_TAG, "Error: ANR file is empty");
                return;
            }
            addAdditionalNumberToAdnRecord(recNum);
        }
    }

    private void readIapFileAndWait(int efid) {
        mFh.loadEFLinearFixedAll(efid, obtainMessage(EVENT_IAP_LOAD_DONE));
        try {
            mLock.wait();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Interrupted Exception in readIapFileAndWait");
        }
    }
   /**
     * Add EF_ANR record to list of AdnRecordEx.
     * @param recNum
     */
    private void addAdditionalNumberToAdnRecord(int recNum) {
        if (mAnrFile == null) {
            return;
        }

        int numAdnRecs = mPhoneBookRecords.size();
        log("addAdditionalNumberToAdnRecord--numAdnRecs = " + numAdnRecs);
        if ((mIapFileRecord != null)&&(mAnrPresentInIap)) {
            // EF_ANR is Type 2.
            for (int i = 0; i < numAdnRecs; i++) {
                byte[] record = null;
                try {
                    record = mIapFileRecord.get(i);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(LOG_TAG, "Error: Improper ICC card: No IAP record for ADN, continuing");
                    break;
                }

                int anrRecNum = record[mAnrTagNumberInIap];
                if ((anrRecNum - 1) > mAnrFile.size()) {
                    continue;
                }

                if (anrRecNum != -1) {
                    // SIM record numbers are 1 based
                    AdnRecord rec = mPhoneBookRecords.get(i);
                    if (rec == null) {
                        rec = new AdnRecord(0, 0, "", "", null);
                    }
                    // Parse EF_ANR record and set to AdnRecordEx object.
                    String additionalNumber = mAnrFile.get(anrRecNum - 1).number;
                    //int extRecNum = mAnrFile.get(anrRecNum - 1).extRecNum;

                    //rec.addAdditionalNumber(i, additionalNumber);
                    //rec.addAnrExtRecordNum(i, extRecNum);
					rec.setAdditionalNumber(additionalNumber);

                    // might be a record with only additional number...
                    mPhoneBookRecords.set(i, rec);
                }
            }
        } else {
            // EF_ANR is Type 1.
            log("addAdditionalNumberToAdnRecord--recNum = " + recNum);
            int i = 0;
            if((recNum == 1)&&(numAdnRecs > 250)){
                i = 250;
            }
            for ( ; i < numAdnRecs; i++) {
                AdnRecord rec = null;
                try {
                    rec = mPhoneBookRecords.get(i);
                    int n = 0;
                    // get additional number and extension record number.
                    for (AnrFile anr : mAnrFile) {
                        // SIM record numbers are 1 based
                       // log("i = "+i+" anr = " + anr.toString() );                        
                        if(anr.adnNumRec != -1) {
                            if ((anr.adnNumRec - 1) == i) {
							rec.setAdditionalNumber(anr.number);
                                //rec.addAdditionalNumber(n, anr.number);
                                //rec.addAnrExtRecordNum(n, anr.extRecNum);
                            }
                        }
                        n++;
                    }
                } catch (IndexOutOfBoundsException e) {
                    break;
                }
                if (rec == null) {
                    continue;
                }

                mPhoneBookRecords.set(i, rec);
            }
        }

        // Read Ext1 record For EF_ANR.
        //readExt1ForAnrFileAndWait(recNum);
        // Lock is canceled after EVENT_ANR_EXT1_LOAD_DONE reception.
    }

      

    private void updatePhoneAdnRecord() {
        if (mEmailFileRecord == null) return;
        int numAdnRecs = mPhoneBookRecords.size();
        if (mIapFileRecord != null) {
            // The number of records in the IAP file is same as the number of records in ADN file.
            // The order of the pointers in an EFIAP shall be the same as the order of file IDs
            // that appear in the TLV object indicated by Tag 'A9' in the reference file record.
            // i.e value of mEmailTagNumberInIap

            for (int i = 0; i < numAdnRecs; i++) {
                byte[] record = null;
                try {
                    record = mIapFileRecord.get(i);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(LOG_TAG, "Error: Improper ICC card: No IAP record for ADN, continuing");
                    break;
                }
                int recNum = record[mEmailTagNumberInIap];

                if (recNum != -1) {
                    //String[] emails = new String[1];
                    // SIM record numbers are 1 based
                    //emails[0] = readEmailRecord(recNum - 1);
					String email = readEmailRecord(recNum - 1);
					
                    AdnRecord rec = mPhoneBookRecords.get(i);
                    if (rec != null) {
                        Log.e(LOG_TAG, "rec != null");
/*2012-02-14-RIL-zhouyi-if the name and number is empty, we should not add any emails-Start*/
                        if(rec.getAlphaTag() == "" && rec.getNumber() == "")
                        {
                        	rec.setEmail(null);
                        }else{
                        rec.setEmail(email);
                        	
                        }
                    } else {
                        Log.e(LOG_TAG, "rec == null");
                        
                       // rec = new AdnRecord("", "", email);
                         rec = new AdnRecord("", "", null);
                    }
                      
/*2012-02-14-RIL-zhouyi-if the name and number is empty, we should not add any emails-End*/
                    mPhoneBookRecords.set(i, rec);
                }
            }
            return;
        }

        // ICC cards can be made such that they have an IAP file but all
        // records are empty. So we read both type 1 and type 2 file
        // email records, just to be sure.

        int len = mPhoneBookRecords.size();
        // Type 1 file, the number of records is the same as the number of
        // records in the ADN file.
        if (mEmailsForAdnRec == null) {
            parseType1EmailFile(len);
        }
        for (int i = 0; i < numAdnRecs; i++) {
            ArrayList<String> emailList = null;
            try {
                emailList = mEmailsForAdnRec.get(i);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            if (emailList == null) continue;

            AdnRecord rec = mPhoneBookRecords.get(i);

            String[] emails = new String[emailList.size()];
            System.arraycopy(emailList.toArray(), 0, emails, 0, emailList.size());
            //rec.setEmails(emails);
			rec.setEmail(emails[0]);
            mPhoneBookRecords.set(i, rec);
        }
    }

    void parseType1EmailFile(int numRecs) {
        mEmailsForAdnRec = new HashMap<Integer, ArrayList<String>>();
        byte[] emailRec = null;
        for (int i = 0; i < numRecs; i++) {
            try {
                emailRec = mEmailFileRecord.get(i);
            } catch (IndexOutOfBoundsException e) {
                Log.e(LOG_TAG, "Error: Improper ICC card: No email record for ADN, continuing");
                break;
            }
            int adnRecNum = emailRec[emailRec.length - 1];

            if (adnRecNum == -1) {
                continue;
            }

            String email = readEmailRecord(i);

            if (email == null || email.equals("")) {
                continue;
            }

            // SIM record numbers are 1 based.
            ArrayList<String> val = mEmailsForAdnRec.get(adnRecNum - 1);
            if (val == null) {
                val = new ArrayList<String>();
            }
            val.add(email);
            // SIM record numbers are 1 based.
            mEmailsForAdnRec.put(adnRecNum - 1, val);
        }
    }

    private String readEmailRecord(int recNum) {
        byte[] emailRec = null;
        try {
            emailRec = mEmailFileRecord.get(recNum);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }

        // The length of the record is X+2 byte, where X bytes is the email address
        String email = IccUtils.adnStringFieldToString(emailRec, 0, emailRec.length - 2);
        return email;
    }

    private void readAdnFileAndWait(int recNum) {
        Map <Integer,Integer> fileIds;
        fileIds = mPbrFile.mFileIds.get(recNum);
        if (fileIds == null || fileIds.isEmpty()) return;


        int extEf = 0;
        // Only call fileIds.get while EFEXT1_TAG is available
        if (fileIds.containsKey(USIM_EFEXT1_TAG)) {
            extEf = fileIds.get(USIM_EFEXT1_TAG);
        }

        mAdnCache.requestLoadAllAdnLike(fileIds.get(USIM_EFADN_TAG),
            extEf, obtainMessage(EVENT_USIM_ADN_LOAD_DONE));
        try {
            mLock.wait();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
        }
    }

    private void createPbrFile(ArrayList<byte[]> records) {
        if (records == null) {
            mPbrFile = null;
            mIsPbrPresent = false;
            return;
        }
        mPbrFile = new PbrFile(records);
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;

        switch(msg.what) {
        case EVENT_PBR_LOAD_DONE:
            ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                createPbrFile((ArrayList<byte[]>)ar.result);
            }
            synchronized (mLock) {
                mLock.notify();
            }
            break;
        case EVENT_USIM_ADN_LOAD_DONE:
            log("Loading USIM ADN records done");
            ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                mPhoneBookRecords.addAll((ArrayList<AdnRecord>)ar.result);
            }
            synchronized (mLock) {
                mLock.notify();
            }
            break;
        case EVENT_IAP_LOAD_DONE:
            log("Loading USIM IAP records done");
            ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                mIapFileRecord = ((ArrayList<byte[]>)ar.result);

		/*2012-02-16-RIL-zhouyi-pick email rec number out of IAP record and store-Start*/
                Iterator<byte[]> it = mIapFileRecord.iterator();
                
                existEmailRecord.clear();//every time clear the record after reloading the IAP
                log("mIapFileRecord.size(): " + mIapFileRecord.size());
                while(it.hasNext())
                {
                	byte[] iapRec = it.next();
                	int recNum = iapRec[mEmailTagNumberInIap];
                	log("all IAP record-recNum: " + recNum);
                    if(recNum != -1)
                    {
                    	log("recNum with email : " +  "recNum == " + recNum);
                    	existEmailRecord.add(recNum);
                    }
                }
                
           /*2012-02-16-RIL-zhouyi-pick email rec number out of IAP record and store-End*/

            }
            synchronized (mLock) {
                mLock.notify();
            }
            break;
        case EVENT_EMAIL_LOAD_DONE:
            log("Loading USIM Email records done");
            ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                if(null == mEmailFileRecord){
                    mEmailFileRecord = ((ArrayList<byte[]>)ar.result);
                }
            }

            if (DBG && mEmailFileRecord != null) {
                int i = 0;
                for (byte[] record : mEmailFileRecord) {
                    log("EF_EMAIL[" + i + "] : " + IccUtils.bytesToHexString(record));
                    i++;
                }
            }

            synchronized (mLock) {
                mLock.notify();
            }
            break;
      case EVENT_ANR_LOAD_DONE:
            log("Loading USIM ANR records done");
            ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                createAnrFile((ArrayList<byte[]>)ar.result);
            }
            synchronized (mLock) {
                mLock.notify();
            }
            break;
        }
    }

    private void createAnrFile(ArrayList<byte[]> records) {
        int i = 0;
        if (records == null) {
            return;
        }

        mAnrFile = new ArrayList<AnrFile>();
        for (byte[] record : records) {
            if (DBG) {
                //log("EF_ANR[" + i + "] : " + IccUtils.bytesToHexString(record));
            }
            mAnrFile.add(new AnrFile(record));
            i++;
        }
    }

    /**
     * ** This New Class Is PMC Customized Coding **<br>
     * This class implements parsing and holding EF_ANR records.
     */
    private class AnrFile {
        String number;
        int adnNumRec = 0xFF;
        int extRecNum = 0;

        AnrFile(byte[] record) {
            try {
                // Length value present in the 2nd byte From first.
                int numberLength = record[1] & 0xff;
                number = PhoneNumberUtils.calledPartyBCDToString(record, 2, numberLength);

                // Adn Record Number present in the Last 1byte.
                //adnNumRec = 0xff & record[record.length - 1];
                adnNumRec = 0xff & record[0];
                // Extension Record Identifier present in the 15th byte.
                extRecNum = 0xff & record[14];

            } catch (RuntimeException ex) {
                Log.w(LOG_TAG, "Error parsing AnrRecord", ex);
            }
        }
        public String toString() {
            return "AnrFile: number :" + number + "  adnNumber: "+ adnNumRec;
        }
    }

    private class PbrFile {
        // RecNum <EF Tag, efid>
        HashMap<Integer,Map<Integer,Integer>> mFileIds;

        PbrFile(ArrayList<byte[]> records) {
            mFileIds = new HashMap<Integer, Map<Integer, Integer>>();
            SimTlv recTlv;
            int recNum = 0;
            for (byte[] record: records) {
                recTlv = new SimTlv(record, 0, record.length);
                parseTag(recTlv, recNum);
                recNum ++;
            }
        }

        void parseTag(SimTlv tlv, int recNum) {
            SimTlv tlvEf;
            int tag;
            byte[] data;
            Map<Integer, Integer> val = new HashMap<Integer, Integer>();
            do {
                tag = tlv.getTag();
                switch(tag) {
                case USIM_TYPE1_TAG: // A8
                case USIM_TYPE3_TAG: // AA
                case USIM_TYPE2_TAG: // A9
                    data = tlv.getData();
                    tlvEf = new SimTlv(data, 0, data.length);
                    parseEf(tlvEf, val, tag);
                    break;
                }
            } while (tlv.nextObject());
            mFileIds.put(recNum, val);
        }

        void parseEf(SimTlv tlv, Map<Integer, Integer> val, int parentTag) {
            int tag;
            byte[] data;
            int tagNumberWithinParentTag = 0;
            do {
                tag = tlv.getTag();
                if (parentTag == USIM_TYPE2_TAG) {
                    if (tag == USIM_EFEMAIL_TAG) {
                        mEmailPresentInIap = true;
                        mEmailTagNumberInIap = tagNumberWithinParentTag;
                    } else if (tag == USIM_EFANR_TAG) {
                        mAnrPresentInIap = true;
                        mAnrTagNumberInIap = tagNumberWithinParentTag;
					}
                }
                switch(tag) {
                    case USIM_EFEMAIL_TAG:
                    case USIM_EFADN_TAG:
                    case USIM_EFEXT1_TAG:
                    case USIM_EFANR_TAG:
                    case USIM_EFPBC_TAG:
                    case USIM_EFGRP_TAG:
                    case USIM_EFAAS_TAG:
                    case USIM_EFGSD_TAG:
                    case USIM_EFUID_TAG:
                    case USIM_EFCCP1_TAG:
                    case USIM_EFIAP_TAG:
                    case USIM_EFSNE_TAG:
                        data = tlv.getData();
                        int efid = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                        val.put(tag, efid);
                        break;
                }
                tagNumberWithinParentTag ++;
            } while(tlv.nextObject());
        }
    }

    private void log(String msg) {
        if(DBG) Log.d(LOG_TAG, msg);
    }
}
