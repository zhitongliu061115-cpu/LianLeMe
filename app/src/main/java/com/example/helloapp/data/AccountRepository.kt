package com.example.helloapp.data

import android.content.Context
import com.example.helloapp.model.Account
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest

class AccountRepository(private val context: Context) {
    private val gson = Gson()
    private val accountsFile get() = context.filesDir.resolve("accounts.json")
    private val currentFile get() = context.filesDir.resolve("current_account.json")

    private fun hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun loadAll(): MutableList<Account> {
        if (!accountsFile.exists()) return mutableListOf()
        val type = object : TypeToken<MutableList<Account>>() {}.type
        return runCatching { gson.fromJson<MutableList<Account>>(accountsFile.readText(), type) ?: mutableListOf() }.getOrDefault(mutableListOf())
    }

    private fun saveAll(list: List<Account>) = accountsFile.writeText(gson.toJson(list))

    fun register(username: String, password: String, displayName: String, avatarPath: String): Boolean {
        val all = loadAll()
        if (all.any { it.username == username }) return false
        all.add(Account(username, hash(password), displayName, avatarPath))
        saveAll(all)
        return true
    }

    fun login(username: String, password: String): Account? {
        val account = loadAll().find { it.username == username && it.passwordHash == hash(password) }
        if (account != null) currentFile.writeText(gson.toJson(account))
        return account
    }

    fun logout() { if (currentFile.exists()) currentFile.delete() }

    fun currentAccount(): Account? = runCatching {
        if (currentFile.exists()) gson.fromJson(currentFile.readText(), Account::class.java) else null
    }.getOrNull()

    fun updateAccount(account: Account) {
        val all = loadAll()
        val idx = all.indexOfFirst { it.username == account.username }
        if (idx >= 0) all[idx] = account else all.add(account)
        saveAll(all)
        currentFile.writeText(gson.toJson(account))
    }
}

