const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// Definišemo konstante radi lakšeg održavanja
const PROXIMITY_RADIUS_METERS = 500; // 500 metara radijus za notifikaciju
const NOTIFICATION_COOLDOWN_MINUTES = 60; // 60 minuta cooldown po izveštaju

// Pomoćna funkcija za računanje distance (Haversine formula)
function getDistanceInMeters(lat1, lon1, lat2, lon2) {
    const R = 6371e3; // Radijus Zemlje u metrima
    const φ1 = lat1 * Math.PI / 180;
    const φ2 = lat2 * Math.PI / 180;
    const Δφ = (lat2 - lat1) * Math.PI / 180;
    const Δλ = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

// NOVA FUNKCIJA: Pokreće se kada se ažurira lokacija korisnika
exports.detectNearbyReports = functions.firestore
    .document("user_locations/{userId}")
    .onUpdate(async (change, context) => {
        const updatedUserData = change.after.data();
        const updatedUserId = context.params.userId;

        // Proveravamo da li korisnik ima lokaciju i FCM token. Ako nema, ne možemo ništa.
        if (!updatedUserData.location || !updatedUserData.fcmToken) {
            console.log(`Korisnik ${updatedUserId} nema lokaciju ili FCM token. Izlazim.`);
            return null;
        }

        const { latitude: userLat, longitude: userLon } = updatedUserData.location;
        const userFcmToken = updatedUserData.fcmToken;

        // Dohvatamo SVE izveštaje o svetlosnom zagađenju iz 'reports' kolekcije
        const allReportsSnapshot = await db.collection("reports").get();

        if (allReportsSnapshot.empty) {
            console.log("Nema prijavljenih izveštaja u bazi.");
            return null;
        }

        const notificationPromises = [];
        const now = new Date();

        // Prolazimo kroz svaki izveštaj
        for (const reportDoc of allReportsSnapshot.docs) {
            const reportData = reportDoc.data();

            // Ako izveštaj nema lokaciju ili ako je korisnik autor izveštaja, preskačemo.
            if (!reportData.location || reportData.userId === updatedUserId) {
                continue;
            }

            const { latitude: reportLat, longitude: reportLon } = reportData.location;

            // Računamo distancu između korisnika i prijavljenog izvora zagađenja
            const distance = getDistanceInMeters(userLat, userLon, reportLat, reportLon);

            // Ako je korisnik u radijusu...
            if (distance <= PROXIMITY_RADIUS_METERS) {
                // ...proveravamo cooldown da ne bismo slali spam notifikacije.
                // Ključ za cooldown je sada kombinacija ID-ja korisnika i ID-ja izveštaja.
                const timestampDocId = `${updatedUserId}_${reportDoc.id}`;
                const timestampRef = db.collection('notification_timestamps').doc(timestampDocId);
                const timestampDoc = await timestampRef.get();

                if (timestampDoc.exists) {
                    const lastNotified = timestampDoc.data().timestamp.toDate();
                    const diffMinutes = (now.getTime() - lastNotified.getTime()) / 60000;
                    if (diffMinutes < NOTIFICATION_COOLDOWN_MINUTES) {
                        console.log(`Cooldown aktivan za korisnika ${updatedUserId} i izveštaj ${reportDoc.id}. Preskačem.`);
                        continue; // Preskačemo ovaj izveštaj i idemo na sledeći
                    }
                }
                
                // Nema cooldown-a, pripremamo notifikaciju!
                console.log(`Korisnik ${updatedUserId} je blizu izveštaja ${reportDoc.id}. Slanje notifikacije.`);

                const message = {
                    notification: {
                        title: "Izvor svetlosnog zagađenja u blizini!",
                        body: `Prijavljeni ${reportData.type || 'izvor'} se nalazi na manje od ${PROXIMITY_RADIUS_METERS} metara od vas.`,
                    },
                    token: userFcmToken,
                };

                // Dodajemo obećanje (Promise) za slanje notifikacije i ažuriranje timestamp-a u niz
                const promise = messaging.send(message).then(async () => {
                    console.log(`USPEH: Notifikacija za izveštaj ${reportDoc.id} poslata korisniku ${updatedUserId}.`);
                    await timestampRef.set({ timestamp: admin.firestore.FieldValue.serverTimestamp() });
                }).catch((error) => {
                    console.error(`GREŠKA pri slanju notifikacije za izveštaj ${reportDoc.id}:`, JSON.stringify(error));
                });

                notificationPromises.push(promise);
            }
        }

        // Čekamo da se sve notifikacije pošalju
        await Promise.all(notificationPromises);
        return null;
    });