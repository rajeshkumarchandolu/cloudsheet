package com.opencloudsheet.model.userdetails

import com.microsoft.identity.client.IAccount

class OneDriveUserDetails(
    private val _account: IAccount,
) : IUserDetails {
    override fun id(): String = _account.id
    override fun email(): String = _account.username
    override fun userName(): String = _account.username
    fun getAccount(): IAccount = _account
}