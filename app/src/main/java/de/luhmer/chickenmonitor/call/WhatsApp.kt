package de.luhmer.chickenmonitor.call

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log


class WhatsApp {

    private val TAG = "[WhatsApp]"

    // https://stackoverflow.com/a/51070920/13370504
    fun makeVideoCall(context: Context, phoneNumber: String) {
        val selection = ContactsContract.RawContacts.ACCOUNT_TYPE + " = 'com.whatsapp' " +
                 "AND " + ContactsContract.Data.MIMETYPE + " = 'vnd.android.cursor.item/vnd.com.whatsapp.video.call' " +
                 "AND " + ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE '%" + phoneNumber + "%'"

        // Log.d(TAG, selection)
        val cursor: Cursor? = context.contentResolver
            .query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data._ID),
                    //null, // query all columns of database
                    selection,
                    null,
                    ContactsContract.Contacts.DISPLAY_NAME
            )

        if (cursor == null) {
            // throw an exception
            Log.e(TAG, "something went wrong when trying to read the cursor")
        } else {
            var id: Long = -1
            while (cursor.moveToNext()) {
                id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID))

                Log.d(TAG, "---------------")
                for (i in 0 until cursor.columnCount) {
                    Log.d(TAG, cursor.getColumnName(i) + " - " + cursor.getString(i))
                }
            }

            if (!cursor.isClosed) {
                cursor.close()
            }

            if(id != -1L) {
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$id"),
                        "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
                )
                intent.setPackage("com.whatsapp")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                Log.d(TAG, "calling now! (contact id: $id)")
                context.startActivity(intent)
            } else {
                Log.d(TAG, "failed to find contact with number $phoneNumber")
            }
        }
    }

    fun markChatAsRead(context: Context, phoneNumber: String) {
        val url = "https://api.whatsapp.com/send?phone=$phoneNumber"
        val i = Intent(Intent.ACTION_VIEW)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        i.data = Uri.parse(url)
        context.startActivity(i)


        /*
        val selection = ContactsContract.RawContacts.ACCOUNT_TYPE + " = 'com.whatsapp' " +
                "AND " + ContactsContract.Data.MIMETYPE + " = 'vnd.android.cursor.item/vnd.com.whatsapp.message' " +
                "AND " + ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE '%" + phoneNumber + "%'"
        Log.d(TAG, selection)
        val cursor: Cursor? = context.contentResolver
                .query(
                        ContactsContract.Data.CONTENT_URI,
                        arrayOf(ContactsContract.Data._ID),
                        //null, // query all columns of database
                        selection,
                        null,
                        ContactsContract.Contacts.DISPLAY_NAME
                )

        if (cursor == null) {
            // throw an exception
            Log.e(TAG, "something went wrong when trying to read the cursor")
        } else {
            var id: Long = -1
            while (cursor.moveToNext()) {
                id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID))

                Log.d(TAG, "---------------")
                for (i in 0 until cursor.columnCount) {
                    Log.d(TAG, cursor.getColumnName(i) + " - " + cursor.getString(i))
                }
            }

            if (!cursor.isClosed) {
                cursor.close()
            }

            if(id != -1L) {
                //  START u0 {act=com.whatsapp.intent.action.OPEN dat=content://com.whatsapp.provider.contact/contacts/2 flg=0x14000000 cmp=com.whatsapp/.Conversation (has extras)
                val intentMarkAsRead = Intent()
                intentMarkAsRead.action = "com.whatsapp.intent.action.OPEN"
                intentMarkAsRead.data = Uri.parse("content://com.whatsapp.provider.contact/contacts/$id")
                intentMarkAsRead.setPackage("com.whatsapp")
                intentMarkAsRead.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                Log.d(TAG, "opening chat now! (contact id: $id)")
                context.startActivity(intentMarkAsRead)
            } else {
                Log.d(TAG, "failed to find contact with number $phoneNumber")
            }
        }
        */
    }
}