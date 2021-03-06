package hu.ait.robinfood

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import hu.ait.robinfood.adapter.OrgsAdapter
import hu.ait.robinfood.data.Organization
import kotlinx.android.synthetic.main.activity_orgs.*

class OrgsActivity : AppCompatActivity() {

    lateinit var orgsAdapter: OrgsAdapter
    lateinit var userOrg: Organization

    var display_type = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orgs)

        setSupportActionBar(toolbar)
        MyThread().start()

        orgsAdapter = OrgsAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        recyclerOrgs.layoutManager = layoutManager

        recyclerOrgs.adapter = orgsAdapter

    }

    inner class MyThread : Thread() {
        override fun run() {
            val db = FirebaseFirestore.getInstance()

            val privateDataRef = db.collection("orgs").document(
                FirebaseAuth.getInstance().currentUser!!.uid)

            val document = Tasks.await(privateDataRef.get())

            if (document.exists()) {
                //Cast the given DocumentSnapshot to our Organization class
                userOrg = document.toObject(Organization::class.java)!!
            } else null

            display_type = if (userOrg!!.type == "Restaurant") "Food pantry" else "Restaurant"
            initOrgs()

        }
    }

    private fun initOrgs() {
        val db = FirebaseFirestore.getInstance()

        val query = db.collection("orgs").whereEqualTo("type", display_type).whereEqualTo(
            "visible", true
        )




        var allOrgsListener = query.addSnapshotListener(
            object: EventListener<QuerySnapshot> {
                override fun onEvent(querySnapshot: QuerySnapshot?, e: FirebaseFirestoreException?) {
                    Log.d("hello", "event")
                    if (e != null) {
                        Toast.makeText(this@OrgsActivity, "listen error: ${e.message}", Toast.LENGTH_LONG).show()
                        return
                    }

                    for (dc in querySnapshot!!.getDocumentChanges()) {
                        when (dc.getType()) {
                            DocumentChange.Type.ADDED -> {
                                val org = dc.document.toObject(Organization::class.java)
                                orgsAdapter.addOrg(org, dc.document.id)
                            }
                            DocumentChange.Type.MODIFIED -> {
                                Toast.makeText(this@OrgsActivity, "update: ${dc.document.id}", Toast.LENGTH_LONG).show()
                            }
                            DocumentChange.Type.REMOVED -> {
                                orgsAdapter.removeOrgByKey(dc.document.id)
                            }
                        }
                    }
                }
            })
    }

//    override fun onBackPressed() {
//        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
//            drawer_layout.closeDrawer(GravityCompat.START)
//        } else {
//            FirebaseAuth.getInstance().signOut()
//            super.onBackPressed()
//        }
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.activity_orgs_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut()
            finish()
        } else if (item?.itemId == R.id.nav_edit_profile) {
            //Maitreyi writes some super cool code here
        }

        return super.onOptionsItemSelected(item)
    }




}
