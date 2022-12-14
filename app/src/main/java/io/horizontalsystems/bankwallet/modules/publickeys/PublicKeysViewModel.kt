package io.horizontalsystems.bankwallet.modules.publickeys

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.hdwalletkit.HDKeychain
import io.horizontalsystems.litecoinkit.MainNetLitecoin

class PublicKeysViewModel(
    account: Account,
) : ViewModel() {
    private val seed: ByteArray?

    init {
        seed = if (account.type is AccountType.Mnemonic) {
            account.type.seed
        } else {
            null
        }
    }

    fun bitcoinPublicKeys(derivation: AccountType.Derivation): String? {
        seed ?: return null
        val network = MainNet()
        val keychain = HDKeychain(seed)

        return keysJson(keychain, purpose(derivation), network.coinType)
    }

    fun bitcoinCashPublicKeys(coinType: BitcoinCashCoinType): String? {
        seed ?: return null
        val keychain = HDKeychain(seed)

        val network = when (coinType) {
            BitcoinCashCoinType.type0 -> MainNetBitcoinCash(MainNetBitcoinCash.CoinType.Type0)
            BitcoinCashCoinType.type145 -> MainNetBitcoinCash(MainNetBitcoinCash.CoinType.Type145)
        }
        return keysJson(keychain, 44, network.coinType)
    }

    fun litecoinPublicKeys(derivation: AccountType.Derivation): String? {
        seed ?: return null
        val network = MainNetLitecoin()
        val keychain = HDKeychain(seed)

        return keysJson(keychain, purpose(derivation), network.coinType)
    }

    fun dashKeys(): String? {
        seed ?: return null
        val network = MainNetDash()
        val keychain = HDKeychain(seed)

        return keysJson(keychain, 44, network.coinType)
    }

    private fun keysJson(
        keychain: HDKeychain,
        purpose: Int,
        coinType: Int,
    ): String {
        val publicKeys = (0..4).map { accountIndex ->
            val key = keychain.getKeyByPath("m/$purpose'/$coinType'/$accountIndex'")
            key.serializePubB58()
        }
        return publicKeys.toString()
    }

    private fun purpose(derivation: AccountType.Derivation): Int = when (derivation) {
        AccountType.Derivation.bip44 -> 44
        AccountType.Derivation.bip49 -> 49
        AccountType.Derivation.bip84 -> 84
    }

}
