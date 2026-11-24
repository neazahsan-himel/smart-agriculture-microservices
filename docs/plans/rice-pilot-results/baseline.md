# Rice Pilot - baseline results

> Run started 2026-11-01 21:19 | farmer `883783ca-8c3e-41d1-aec4-44ce4a0685e0` (Karim Saheb (pilot)) | gateway http://localhost:8080
> Questions: 25 | scoring criteria S1-S8 in `docs/plans/rice-pilot-test-questions.md` section 3

---

## F1 (EN, topic `CROP_RECOMMENDATION`)

- Time: 547 s | turns: 1
- Diagnostic case: risk `LOW`, confidence `HIGH`, status `OPEN`
- Tasks created/changed: none

**Farmer:**

> How much urea should I apply per bigha for Boro rice, and in how many splits?

**Advisor:**

> For Boro rice (Kharif season), apply **25-30 kg urea per bigha** in **3 splits**:  
> - First split at 20-30 days after transplanting (tillering stage)  
> - Second split at 45-50 days (panicle initiation)  
> - Third split at 60-70 days (flowering stage)  
> 
> **Apply on Tue 3 Nov 06:00-09:00 (early morning)** for dry conditions. Use a broadcast spreader for even coverage.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## F2 (BN, topic `CROP_RECOMMENDATION`)

- Time: 345 s | turns: 1
- Diagnostic case: risk `MEDIUM`, confidence `MEDIUM`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: ; SPRAY: Best time to spray: Mon 2 Nov 06:00.

**Farmer:**

> আমন ধানে টিএসপি আর এমওপি কখন দেব? জমি চাষের সময় নাকি পরে?

**Advisor:**

> For your Boro rice (Kharif season), apply **TSP (phosphorus)** and **MOP (potassium)** during the **flowering stage** (60-70 days after transplanting). Based on your farm memory, this aligns with the **third urea split**. Apply them **together** as a top dressing for balanced nutrients.  
> 
> **Recommended timing:**  
> - **Tue 3 Nov 06:00-09:00 (early morning)** — dry conditions, ideal for application. Use a broadcast spreader for even coverage.  
> 
> **Action:**  
> - Apply TSP and MOP now (Tue 3 Nov) during the early morning window.
> 
> Forecast check: Best time to spray: Mon 2 Nov 06:00.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## F3 (EN, topic `CROP_RECOMMENDATION`)

- Time: 451 s | turns: 0
- Tasks created/changed: none
- **ERROR:** The remote server returned an error: (500) Internal Server Error.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## F4 (BN, topic `GENERAL`)

- Time: 264 s | turns: 1
- Diagnostic case: risk `MEDIUM`, confidence `MEDIUM`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: ; SPRAY: Best time to spray: Mon 2 Nov 06:00.

**Farmer:**

> ধানের রং ফ্যাকাশে, শ্রমিক পেয়েছি, কিন্তু কাল-পরশু ভারী বৃষ্টির সম্ভাবনা। আজ ইউরিয়া দেব?

**Advisor:**

> Based on your situation, here's the recommendation:
> 
> - **Apply urea today (Mon 2 Nov) during the early morning window (06:00-09:00)** when conditions are dry and rain is minimal. This avoids washout risk from the drizzle expected midday.  
> - **Mix urea with water** to ensure even coverage and reduce the risk of fertilizer burn.  
> - **Monitor the crop closely** for any signs of stress after application, as heavy rain on Tue 3 Nov could still impact nutrient uptake.
> 
> Forecast check: Best time to spray: Mon 2 Nov 06:00.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## F5 (EN, topic `CROP_RECOMMENDATION`)

- Time: 474 s | turns: 2
- Diagnostic case: risk `MEDIUM`, confidence `LOW`, status `OPEN`
- Tasks created/changed: none

**Farmer:**

> Do I need to apply gypsum and zinc for rice in Gazipur?

**Advisor:**

> To determine if gypsum and zinc are needed, I need more details: Are you experiencing specific symptoms like yellowing leaves, stunted growth, or low yields that might indicate nutrient deficiencies?

**Farmer (context):**

> I have not done a soil test recently.

**Advisor:**

> To determine if gypsum and zinc are needed, I need more details: Are you experiencing specific symptoms like yellowing leaves, stunted growth, or low yields that might indicate nutrient deficiencies?

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## F6 (EN, topic `CROP_RECOMMENDATION`)

- Time: 415 s | turns: 1
- Diagnostic case: risk `MEDIUM`, confidence `MEDIUM`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: SPRAY: Best time to spray: Mon 2 Nov 06:00.; FOLLOW_UP: 

**Farmer:**

> I applied DAP instead of TSP at land preparation. Should I change the urea dose?

**Advisor:**

> Based on your situation, here's the recommendation:
> 
> - **Apply the third urea split (25-30 kg urea per bigha) on Tue 3 Nov 06:00-09:00** during the early morning window when conditions are dry. This avoids washout risk from the drizzle expected midday.  
> - **Reduce urea dose slightly** if you already applied DAP during land preparation (DAP provides nitrogen), to prevent over-fertilization. Monitor crop response for yellowing or burn symptoms.  
> - **Mix urea with water** for even coverage and reduce fertilizer burn risk.  
> 
> Forecast check: Best time to spray: Tue 3 Nov 06:00.
> 
> Forecast check: Best time to spray: Mon 2 Nov 06:00.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## F7 (BN, topic `CROP_RECOMMENDATION`)

- Time: 379 s | turns: 1
- Diagnostic case: risk `MEDIUM`, confidence `HIGH`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: SPRAY: Best time to spray: Mon 2 Nov 06:00.; FOLLOW_UP: 

**Farmer:**

> এক বিঘা বোরো ধানে মোট কত কেজি সার লাগবে? ইউরিয়া, টিএসপি, পটাশ সব মিলিয়ে।

**Advisor:**

> For 1 bigha of Boro rice, apply:  
> - **Urea**: 60-75 kg (split into 3 doses: 20-25 kg each, with the third split on Tue 3 Nov 06:00-09:00).  
> - **TSP**: 30-40 kg (apply as top dressing during flowering stage, ideally Tue 3 Nov 06:00-09:00).  
> - **MOP**: 40-50 kg (apply with TSP for balanced nutrients, same timing).  
> 
> **Action:**  
> - Apply urea splits and TSP/MOP together on Tue 3 Nov 06:00-09:00 (dry early morning). Avoid midday drizzle.  
> - Mix urea with water to prevent burn.
> 
> Forecast check: Best time to spray: Mon 2 Nov 06:00.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## F8 (EN, topic `CROP_RECOMMENDATION`)

- Time: 289 s | turns: 1
- Diagnostic case: risk `LOW`, confidence `MEDIUM`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: 

**Farmer:**

> Can I mix urea and potash and broadcast them together?

**Advisor:**

> Mixing urea and potash is acceptable, but ensure they are dissolved separately in water before combining to avoid clumping. Apply during dry periods (e.g., Tue 3 Nov 06:00-09:00) to prevent nutrient runoff.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## F9 (EN, topic `CROP_RECOMMENDATION`)

- Time: 517 s | turns: 2
- Diagnostic case: risk `MEDIUM`, confidence `MEDIUM`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: ; SPRAY: Best time to spray: Mon 2 Nov 06:00.

**Farmer:**

> One of my three rice plots (north side) has shorter, paler plants and uneven growth. Same variety, same fertiliser, same planting date. Should I add more urea there?

**Advisor:**

> The shorter, paler plants and uneven growth in your north plot could indicate a nutrient deficiency or localized stress. To narrow down the cause, I need more details: Are the plants showing yellowing (especially between veins), stunted growth, or any signs of pests/diseases? This will help determine if the issue is nitrogen-related or something else.

**Farmer (context):**

> That plot also yielded less last year, and it stays wetter than the others.

**Advisor:**

> Based on the wetter conditions and lower yield history, the north plot likely suffers from waterlogged soil or nutrient leaching. Apply the third urea split (25-30 kg/acre) during the dry early morning window on Tue 3 Nov 06:00-09:00 to avoid washout. Check for nitrogen deficiency (yellowing between veins) and improve drainage if waterlogging persists.
> 
> Forecast check: Best time to spray: Mon 2 Nov 06:00.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## F10 (EN, topic `CROP_RECOMMENDATION`)

- Time: 280 s | turns: 1
- Diagnostic case: risk `MEDIUM`, confidence `MEDIUM`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: 

**Farmer:**

> Is cow dung enough instead of chemical fertiliser for Aman rice?

**Advisor:**

> Using cow dung alone may not provide enough nutrients for Aman rice, especially for nitrogen, which is critical for leaf growth. While cow dung adds organic matter and some nutrients, it may not meet the high nitrogen demands of Aman rice during the monsoon season. For optimal growth, combine cow dung with **urea splits** (as previously advised) to ensure sufficient nitrogen supply.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

