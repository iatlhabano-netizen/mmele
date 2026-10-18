# Real-money play: NOT enabled

`REAL_MONEY_ENABLED = false` (`app/build.gradle.kts` buildConfigField and `online/.../ranked/Ranked.kt`).
There is **no payment code** in Mmele; nothing reads the flag except a test that asserts it is false.
Points are virtual and have no cash value. Switching the flag on alone would do nothing — and must not be done
without all of the following.

## What real-money (or prize) play would require
1. **Gambling/gaming licence.** In Botswana, skill or chance games for stakes fall under the Gambling Act (2012)
   and the Gambling Authority; other countries need their own licences. Get legal advice first.
2. **Google Play policy.** Real-money gambling apps are allowed only in approved countries, with an approved
   licence, a separate Play application form, age-gating and geofencing. Prize tournaments have their own policy.
3. **Age and identity checks (KYC, 18+).** Verified identity, age 18+, sanctions/PEP screening, self-exclusion and
   deposit limits (responsible-gaming tools).
4. **Payment provider** licensed for gaming (deposits, withdrawals, refunds, chargebacks), PCI-DSS handled by the
   provider. Google Play Billing cannot be used for gambling.
5. **Server-side ledger:** double-entry, immutable, audited, reconciled with the provider; real accounts
   (not anonymous auth); anti-fraud and anti-collusion (move replay on the server, device checks).
6. **Tax and AML**: reporting, anti-money-laundering procedures, record keeping.
7. **Updated privacy policy, terms and Data safety** (financial and personal info).

Until then, keep the flag false and keep the "no cash value" text on every points screen.
