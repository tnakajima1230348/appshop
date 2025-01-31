package com.example.appshop

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.net.URI
import org.json.JSONObject

class RestaurantLogin : AppCompatActivity() {
    companion object{
        const val loginReqId: Int = 1
        var loginRestaurantName: String = ""
    }

    private val uri = WsClient.serverRemote
    private var client = LoginWsClient(this, uri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_login)
    }

    override fun onResume() {
        super.onResume()
        client.connect()

        //edit text, name and password
        val eTxtRestaurantName: EditText = findViewById(R.id.textBoxRestaurantName)
        val eTxtPassword: EditText = findViewById(R.id.textBoxPassword)
        val errorDisplay: TextView = findViewById(R.id.errorDisplay)

        val buttonLogin: Button = findViewById(R.id.buttonLogin)
        val buttonCreateAcc: Button = findViewById(R.id.buttonCreateAccount)
        //eventListener
        buttonLogin.setOnClickListener {
            val loginParams = JSONObject()
            val restaurantName: String = eTxtRestaurantName.text.toString()
            val password: String = eTxtPassword.text.toString()
            val role = "restaurant"
            loginRestaurantName = restaurantName

            loginParams.put("user_name", restaurantName)
            loginParams.put("password", password)
            loginParams.put("role", role)

            val loginRequest = client.createJsonrpcReq("login", loginReqId, loginParams)

            Log.i(javaClass.simpleName, "send login req")
            Log.i(javaClass.simpleName, loginRequest.toString())

            try{
                if(client.isClosed) {
                    client.reconnect()
                }
                client.send(loginRequest.toString())
            } catch (ex: Exception){
                Log.i(javaClass.simpleName, "send failed $ex")
                errorDisplay.visibility = View.VISIBLE
                errorDisplay.text = "インターネットに接続されていません"
            }
        }

        buttonCreateAcc.setOnClickListener {
            val intent = Intent(this@RestaurantLogin, RestaurantRegisterAccount::class.java)
            startActivity(intent)
        }
    }

    override fun onRestart() {
        super.onRestart()
        client = LoginWsClient(this, uri)
    }
}

class LoginWsClient(private val activity: Activity, uri: URI) : WsClient(uri){

    private val errorDisplay : TextView by lazy{
        activity.findViewById(R.id.errorDisplay)
    }

    override fun onMessage(message: String?) {
        super.onMessage(message)
        Log.i(javaClass.simpleName, "msg arrived")
        Log.i(javaClass.simpleName, "$message")
        val wholeMsg = JSONObject("$message")
        val resId: Int = wholeMsg.getInt("id")
        val result: JSONObject = wholeMsg.getJSONObject("result")
        val status: String = result.getString("status")

        //if message is about login
        if(resId == RestaurantLogin.loginReqId){
            if(status == "success"){
                val token: String = result.getString("token")
                val expire: String = result.getString("expire")
                Log.i(javaClass.simpleName, "login success")
                Log.i(javaClass.simpleName, "token: $token")
                Log.i(javaClass.simpleName, "expires in $expire")

                this.close(NORMAL_CLOSURE)
                activity.runOnUiThread{
                    val intent = Intent(activity, Restaurant::class.java)
                    Restaurant.globalToken = token
                    Restaurant.globalRestaurantName = RestaurantLogin.loginRestaurantName
                    Restaurant.globalTokenExpiry = expire
                    activity.startActivity(intent)
                }

            }else if(status == "error"){
                activity.runOnUiThread{
                    val reason: String = result.getString("reason")
                    errorDisplay.visibility = View.VISIBLE
                    Log.i(javaClass.simpleName, "login failed with reason $reason")
                }
            }
        }
    }
}
