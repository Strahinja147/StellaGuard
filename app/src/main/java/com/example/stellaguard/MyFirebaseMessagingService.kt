package com.example.stellaguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Pokreće se kada stigne notifikacija dok je aplikacija u prvom planu
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Primljena poruka: ${remoteMessage.notification?.title}")

        // Kada je aplikacija u prvom planu, moramo ručno prikazati notifikaciju.
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    // Funkcija za kreiranje i prikazivanje notifikacije
    private fun showNotification(title: String?, body: String?) {
        val channelId = "stellaguard_notification_channel" // Jedinstveni ID za vaš kanal
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Kreiranje Notification Channel-a je obavezno za Android 8.0 (Oreo) i novije
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nearby User Alerts", // Ime kanala koje korisnik vidi u podešavanjima
                NotificationManager.IMPORTANCE_HIGH // Visok prioritet da bi notifikacija "iskočila"
            ).apply {
                description = "Notifications when another user is nearby."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Kreiranje same notifikacije
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // VAŽNO: Kreirajte ikonicu 'ic_notification.xml' u res/drawable folderu.
            // Može biti bilo koja mala, jednobojna (bela) ikonica.
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Zamenite sa vašom ikonicom
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true) // Notifikacija se uklanja kada se klikne na nju
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Prikazivanje notifikacije sa jedinstvenim ID-jem (npr. 0)
        notificationManager.notify(0, notificationBuilder.build())
    }


    // Pokreće se svaki put kada se generiše novi token za uređaj
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Novi token: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        // Kada dobijemo token, odmah ga sačuvajmo u Firestore
        // za trenutno ulogovanog korisnika.
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userLocationRef = FirebaseFirestore.getInstance()
                .collection("user_locations").document(uid)

            // Koristimo merge da ne bismo pregazili lokaciju ako već postoji
            userLocationRef.set(mapOf("fcmToken" to token), SetOptions.merge())
                .addOnSuccessListener { Log.d("FCM", "Token uspešno sačuvan u Firestore.") }
                .addOnFailureListener { e -> Log.e("FCM", "Greška pri čuvanju tokena.", e) }
        }
    }
}