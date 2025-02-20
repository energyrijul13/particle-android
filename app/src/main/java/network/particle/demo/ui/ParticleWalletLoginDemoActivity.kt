package network.particle.demo.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.connect.common.ConnectCallback
import com.connect.common.model.Account
import com.connect.common.model.ChainType
import com.connect.common.model.ConnectError
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import com.minijoy.demo.R
import com.minijoy.demo.databinding.ActivityParticleWalletDemoBinding
import com.particle.api.infrastructure.db.table.Wallet
import com.particle.api.infrastructure.db.table.WalletType
import com.particle.api.service.DBService.walletDao
import com.particle.base.*
import com.particle.base.EthereumChain
import com.particle.base.EthereumChainId
import com.particle.base.SolanaChain
import com.particle.base.SolanaChainId
import com.particle.base.data.WebServiceCallback
import com.particle.base.data.WebServiceError
import com.particle.connect.ParticleConnect
import com.particle.gui.ParticleWallet
import network.particle.demo.ui.adapter.BannerAdapter
import com.particle.gui.router.PNRouter
import com.particle.gui.ui.login.LoginTypeCallBack
import com.particle.gui.ui.setting.manage_wallet.dialog.WallectConnectTabCallback
import com.particle.gui.ui.setting.manage_wallet.dialog.WalletConnectTabFragment
import com.particle.gui.ui.setting.manage_wallet.private_login.PrivateKeyLoginActivity
import com.particle.gui.utils.WalletUtils
import com.particle.network.ParticleNetworkAuth.getAddress
import com.particle.network.ParticleNetworkAuth.login
import com.particle.network.service.LoginType
import com.particle.network.service.SupportAuthType
import com.particle.network.service.model.LoginOutput
import com.wallet.connect.adapter.model.MobileWCWallet
import com.zhpan.bannerview.constants.IndicatorGravity
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import kotlinx.coroutines.launch
import network.particle.demo.ui.base.DemoBaseActivity


class ParticleWalletLoginDemoActivity :
    DemoBaseActivity<ActivityParticleWalletDemoBinding>(R.layout.activity_particle_wallet_demo) {
    val pageTipsStr = arrayOf(
        R.string.pn_page1_tips,
        R.string.pn_page2_tips,
        R.string.pn_page3_tips,
        R.string.pn_page4_tips
    )

    override fun initView() {
        super.initView()
        ImmersionBar.with(this).transparentStatusBar().hideBar(BarHide.FLAG_HIDE_BAR).init()
        initHorizontalBanner()
        setObserver()
    }

    lateinit var launcherResult: ActivityResultLauncher<Intent>
    override fun setListeners() {
        super.setListeners()
        binding.rlLoginEmail.setOnClickListener {
            loginWithPn(LoginType.EMAIL)
        }
        binding.rlLoginMetaMask.setOnClickListener {
            connectEvmWallet(MobileWCWallet.MetaMask)
        }
        binding.ivGoogle.setOnClickListener {
            loginWithPn(LoginType.GOOGLE)
        }
        binding.ivFacebook.setOnClickListener {
            loginWithPn(LoginType.FACEBOOK)
        }
        binding.ivApple.setOnClickListener {
            loginWithPn(LoginType.APPLE)
        }
//        binding.ivTwitter.setOnClickListener {
//            loginWithPn(LoginType.TWITTER)
//        }
        binding.ivOther.setOnClickListener {
            PNRouter.navigatorLoginList(supportFragmentManager, object : LoginTypeCallBack {
                override fun onLoginType(loginType: LoginType) {
                    loginWithPn(loginType)
                }

                override fun onLoginConnect(walletType: WalletType) {
                    when (walletType) {
                        WalletType.CONNET_METAMASK -> {
                            connectEvmWallet(MobileWCWallet.MetaMask)
                        }

                        WalletType.CONNET_RAINBOW -> {
                            connectEvmWallet(MobileWCWallet.Rainbow)
                        }

                        WalletType.CONNET_TRUST -> {
                            connectEvmWallet(MobileWCWallet.Trust)
                        }

                        WalletType.CONNET_IMTOKEN -> {
                            connectEvmWallet(MobileWCWallet.ImToken)
                        }

                        WalletType.CONNET_BITKEEP -> {
                            connectEvmWallet(MobileWCWallet.BitKeep)
                        }

                        WalletType.CONNET_WALLET -> {
                            walletConnect()
                        }

                        WalletType.CONNET_PHANTOM -> {
                            connectPhantom()
                        }

                        WalletType.SOL_IMPORT -> {
                            val intent =
                                PrivateKeyLoginActivity.newIntent(this@ParticleWalletLoginDemoActivity)
                            launcherResult.launch(intent)
                        }

                        WalletType.ETH_IMPORT -> {
                            val intent =
                                PrivateKeyLoginActivity.newIntent(this@ParticleWalletLoginDemoActivity)
                            launcherResult.launch(intent)
                        }

                        else -> {

                        }
                    }
                }

            })
        }
    }


    private fun initHorizontalBanner() {
        binding.bannerView.setScrollDuration(600).setOffScreenPageLimit(2)
            .setLifecycleRegistry(lifecycle).setIndicatorStyle(IndicatorStyle.CIRCLE)
            .setIndicatorSlideMode(IndicatorSlideMode.NORMAL).setInterval(3000)
            .setIndicatorGravity(IndicatorGravity.CENTER).setIndicatorSliderRadius(10)
            .disallowParentInterceptDownEvent(true).setIndicatorSliderColor(
                Color.parseColor("#4E4E50"), Color.WHITE
            ).setAdapter(BannerAdapter())
            .registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.tvSubTitle.text = getString(pageTipsStr[position])
                }
            }).create()
        binding.bannerView.refreshData(
            mutableListOf(
                R.drawable.page1, R.drawable.page2, R.drawable.page3, R.drawable.page4
            )
        )

    }

    private fun loginWithPn(
        loginType: LoginType, supportAuthType: SupportAuthType = SupportAuthType.ALL
    ) {
        ParticleNetwork.login(loginType,
            supportAuthType.value,
            false,
            loginCallback = object : WebServiceCallback<LoginOutput> {
                override fun success(output: LoginOutput) {
                    lifecycleScope.launch {
                        val wallet = Wallet.createPnWallet(
                            ParticleNetwork.getAddress(),
                            ParticleNetwork.chainInfo.chainName.toString(),
                            ParticleNetwork.chainInfo.chainId.toString(),
                            1,
                        )
                        ParticleWallet.setWallet(wallet)
                        if(ParticleNetwork.isBiconomyModeEnable()){
                            //if you want to use 4337 mode, you need syncSmartAccounts before open Wallet
                            val wallets = walletDao.getAllWallets()
                            val addresses = wallets.map { it.address }
                            ParticleNetwork.getBiconomyService().getSmartAccounts(addresses)
                        }
                        openWallet()
                    }
                }

                override fun failure(errMsg: WebServiceError) {
                }

            })
    }


    private fun connectEvmWallet(mobileWallet: MobileWCWallet) {
        ParticleConnect.setChain(EthereumChain(EthereumChainId.Mainnet))
        val adapter =
            ParticleConnect.getAdapters(ChainType.EVM).first { it.name == mobileWallet.name }
        adapter.connect(null, object : ConnectCallback {
            override fun onConnected(account: Account) {
                lifecycleScope.launch {
                    val wallet = WalletUtils.createSelectedWallet(account.publicAddress, adapter)
                    ParticleWallet.setWallet(wallet)
                    openWallet()
                }
            }

            override fun onError(error: ConnectError) {
            }
        })
    }

    private fun walletConnect() {
        ParticleConnect.setChain(EthereumChain(EthereumChainId.Mainnet))
        val qrFragment = WalletConnectTabFragment()
        qrFragment.setConnectCallback(object : WallectConnectTabCallback {
            override fun onConnect(account: Account, wallectType: WalletType) {
                qrFragment.dismissAllowingStateLoss()
                lifecycleScope.launch {
                    val wallet =
                        WalletUtils.createSelectedWallet(account.publicAddress, wallectType)
                    ParticleWallet.setWallet(wallet)
                    openWallet()
                }
            }

            override fun onConnectError(error: ConnectError) {
                qrFragment.dismissAllowingStateLoss()
            }
        })
        qrFragment.show(supportFragmentManager, "WalletConnectTabFragment")
    }

    private fun connectPhantom() {
        ParticleConnect.setChain(SolanaChain(SolanaChainId.Mainnet))
        val adapter = ParticleConnect.getAdapters(ChainType.Solana).first { it.name == "Phantom" }
        adapter.connect(null, object : ConnectCallback {
            override fun onConnected(account: Account) {
                lifecycleScope.launch {
                    val wallet = WalletUtils.createSelectedWallet(account.publicAddress, adapter)
                    ParticleWallet.setWallet(wallet)

                    openWallet()
                }
            }

            override fun onError(error: ConnectError) {
            }
        })
    }

    override fun setObserver() {
        launcherResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                if (activityResult.resultCode == Activity.RESULT_OK) {
                    openWallet()
                }
            }
    }

    private fun openWallet() {
        finish()
    }

}