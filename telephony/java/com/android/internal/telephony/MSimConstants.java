/* Copyright (c) 2011-12, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.internal.telephony;

public class MSimConstants {
    public static final int RIL_MAX_CARDS        = 2;

    public static final int NUM_SUBSCRIPTIONS    = 2;

    public static final int DEFAULT_SUBSCRIPTION = 0;

    public static final int RIL_CARD_MAX_APPS    = 8;

    public static final int DEFAULT_CARD_INDEX   = 0;

    public static final int MAX_PHONE_COUNT_DS   = 2;

    public static final String SUBSCRIPTION_KEY  = "subscription";

    public static final int SUB1 = 0;
    public static final int SUB2 = 1;

    public static final int EVENT_SUBSCRIPTION_ACTIVATED   = 500;
    public static final int EVENT_SUBSCRIPTION_DEACTIVATED = 501;

    public enum CardUnavailableReason {
        REASON_CARD_REMOVED,
        REASON_RADIO_UNAVAILABLE,
        REASON_SIM_REFRESH_RESET
    };
}
