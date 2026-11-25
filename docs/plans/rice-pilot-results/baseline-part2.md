# Rice Pilot - baseline-part2 results

> Run started 2026-11-24 20:01 | farmer `8710ac31-7d66-4df5-a52c-db2638b7059c` (Karim Saheb (pilot)) | gateway http://localhost:8080
> Questions: 16 | scoring criteria S1-S8 in `docs/plans/rice-pilot-test-questions.md` section 3

---

## F3 (EN, topic `CROP_RECOMMENDATION`)

- Time: 545 s | turns: 1
- Diagnostic case: risk `MEDIUM`, confidence `MEDIUM`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: ; SPRAY: Best time to spray: Wed 25 Nov 06:00.

**Farmer:**

> My rice leaves are pale yellow, 25 days after transplanting. Should I top-dress urea now?

**Advisor:**

> The pale yellow leaves at 25 days post-transplanting suggest nitrogen deficiency, which is common during tillering. Urea top-dressing is a valid approach, but timing and method matter.  
> 
> - **Apply 20-30 kg urea per hectare** as a soil drench (not foliar spray) to ensure root uptake.  
> - **Spray early morning (Wed 25 Nov 06:00-09:00)** to avoid heat stress and maximize absorption.  
> - **Avoid over-application** (more than 40 kg/ha) to prevent leaf burn.
> 
> Forecast check: Best time to spray: Wed 25 Nov 06:00.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## P3 (EN, topic `PEST_CONTROL`)

- Time: 491 s | turns: 2
- Diagnostic case: risk `MEDIUM`, confidence `LOW`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: 

**Farmer:**

> I saw a few insects on one side of my field. Should I spray now?

**Advisor:**

> The insects you've seen could be a variety of pests—could you share what they look like (e.g., aphids, caterpillars, beetles) and how much of the field is affected? This will help determine the best action.

**Farmer (context):**

> I have not identified the insect yet. It is a small area.

**Advisor:**

> The pale yellow leaves at 25 days post-transplanting suggest nitrogen deficiency, which is common during tillering. Urea top-dressing is a valid approach, but timing and method matter.  
> 
> Apply 20-30 kg urea per hectare as a soil drench (not foliar spray) to ensure root uptake.  
> Spray early morning (Wed 25 Nov 06:00-09:00) to avoid heat stress and maximize absorption.  
> Avoid over-application (more than 40 kg/ha) to prevent leaf burn.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## P5 (BN, topic `PEST_CONTROL`)

- Time: 180 s | turns: 1
- Diagnostic case: risk `MEDIUM`, confidence `LOW`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: 

**Farmer:**

> ধানের মাঝের ডগা মরে যাচ্ছে, টান দিলে সহজে উঠে আসে। কী করব?

**Advisor:**

> The farmer mentions rice plants with dying middle stems that pull easily. This could indicate stem rot, lodging (falling over), or pest damage. To diagnose accurately, I need more details.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## P6 (EN, topic `PEST_CONTROL`)

- Time: 284 s | turns: 1
- Diagnostic case: risk `MEDIUM`, confidence `MEDIUM`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: 

**Farmer:**

> After flowering, some panicles are white and empty. What caused this, and can I still save the crop?

**Advisor:**

> The white, empty panicles after flowering likely indicate **grain filling failure**, often caused by nutrient imbalance (especially potassium deficiency), water stress, or pest damage. Since the farmer already applied urea (nitrogen) earlier, the issue may now be potassium or boron deficiency, or environmental stress during flowering.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## P7 (EN, topic `PEST_CONTROL`)

- Time: 837 s | turns: 2
- Diagnostic case: risk `MEDIUM`, confidence `MEDIUM`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: SPRAY: Best time to spray: Wed 25 Nov 06:00.; FOLLOW_UP: 

**Farmer:**

> Leaves are turning yellow-orange from the tip and plants are stunted in patches. Is it a virus?

**Advisor:**

> The yellow-orange discoloration and stunted growth could indicate a nutrient deficiency (like potassium or boron), pest damage, or a virus. Viruses often cause uneven symptoms, but without seeing the pattern or knowing if the issue is spreading, it’s hard to confirm.  
> 
> Could you share:  
> - Are the yellowing leaves in patches or widespread?  
> - Are there any insects (e.g., aphids, caterpillars) on the plants?

**Farmer (context):**

> It is Aman rice at the tillering stage. I have seen leafhoppers.

**Advisor:**

> The yellow-orange discoloration and stunted growth, combined with leafhopper presence, strongly suggests **Rice Yellow Mosaic Virus (RYMV)** or **leafhopper-induced stress**. Leafhoppers transmit RYMV, which causes patchy yellowing and stunting. Nutrient deficiencies (like potassium) could worsen symptoms but are secondary here.  
> 
> **Recommendations:**  
> - **Spray insecticide** (e.g., lambda-cyhalothrin or malathion) **Wed 25 Nov 06:00-09:00** to target leafhoppers before virus spread.  
> - **Apply potassium-rich fertilizer** (e.g., potassium sulfate, 20-30 kg/ha) as a soil drench **Wed 25 Nov 06:00-09:00** to counteract stress.  
> - **Monitor for virus symptoms** (e.g., mosaic patterns, leaf curling) over the next 7 days.
> 
> Forecast check: Best time to spray: Wed 25 Nov 06:00.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## P8 (BN, topic `PEST_CONTROL`)

- Time: 511 s | turns: 2
- Diagnostic case: risk `MEDIUM`, confidence `LOW`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: 

**Farmer:**

> গত বছর শীষ বের হওয়ার সময় এই জমিতে রোগ হয়েছিল। এবার কয়েকদিন ধরে বৃষ্টি আর আর্দ্রতা বেশি। আগে থেকে কিছু করব?

**Advisor:**

> The recent rain and humidity could worsen pest/disease risks. Last year's issue (flowering-stage damage) might recur if leafhoppers or pathogens are present.  
> 
> Are the yellowing leaves in patches or widespread? Are there any insects (e.g., leafhoppers, aphids) on the plants?

**Farmer (context):**

> শীষ বের হওয়ার সময় কাছাকাছি। গত বছরের একই জাত।

**Advisor:**

> The rice is in the flowering stage, same as last year, and recent rain/humidity may have worsened pest/disease risks. To confirm if leafhoppers (which transmit RYMV) are present, I need to know:  
> Are there small, greenish insects (leafhoppers) or aphids on the plants?

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

---

## V1 (EN, topic `CROP_RECOMMENDATION`)

- Time: 235 s | turns: 1
- Diagnostic case: risk `MEDIUM`, confidence `MEDIUM`, status `AWAITING_FOLLOWUP`
- Tasks created/changed: FOLLOW_UP: 

**Farmer:**

> Which rice variety should I plant this Aman season in a low field that floods for a week or two?

**Advisor:**

> Based on your field's flooding conditions and the previous pest/disease history, here's a recommendation:
> 
> **Recommendations:**  
> - **Plant flood-tolerant rice varieties** like BRRI Dhan 56 or BRRI Dhan 65, which survive 1-2 weeks of submergence.  
> - **Monitor for leafhoppers** (small green insects) weekly; if present, spray lambda-cyhalothrin **Wed 25 Nov 06:00-09:00** (best time per forecast).  
> - **Add potassium sulfate** (20-30 kg/ha) during land leveling to prevent stress from flooding and pests.

| S1 registered | S2 dose | S3 source | S4 admits gap | S5 clarifies | S6 safety | S7 timing | S8 rating |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

