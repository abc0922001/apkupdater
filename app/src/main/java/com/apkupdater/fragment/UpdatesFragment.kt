package com.apkupdater.fragment

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.apkupdater.R
import com.apkupdater.model.AppUpdate
import com.apkupdater.util.InstallUtil
import com.apkupdater.util.adapter.BindAdapter
import com.apkupdater.util.getAccentColor
import com.apkupdater.util.ioScope
import com.apkupdater.util.launchUrl
import com.apkupdater.util.observe
import com.apkupdater.viewmodel.MainViewModel
import com.apkupdater.viewmodel.UpdatesViewModel
import kotlinx.android.synthetic.main.fragment_apps.recycler_view
import kotlinx.android.synthetic.main.view_apps.view.action_one
import kotlinx.android.synthetic.main.view_apps.view.icon
import kotlinx.android.synthetic.main.view_apps.view.name
import kotlinx.android.synthetic.main.view_apps.view.packageName
import kotlinx.android.synthetic.main.view_apps.view.progress
import kotlinx.android.synthetic.main.view_apps.view.source
import kotlinx.android.synthetic.main.view_apps.view.version
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.sharedViewModel

class UpdatesFragment : Fragment() {

	private val updatesViewModel: UpdatesViewModel by sharedViewModel()
	private val mainViewModel: MainViewModel by sharedViewModel()
	private val installer: InstallUtil by inject()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
		inflater.inflate(R.layout.fragment_updates, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		recycler_view.layoutManager = LinearLayoutManager(context)
		val adapter = BindAdapter(R.layout.view_apps, onBind)
		recycler_view.adapter = adapter
		(recycler_view.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
		updatesViewModel.items.observe(this) {
			it?.let {
				adapter.items = it
				mainViewModel.updatesBadge.postValue(it.size)
			}
		}
	}

	private val onBind = { view: View, app: AppUpdate ->
		view.name.text = app.name
		view.packageName.text = app.packageName
		view.version.text = getString(R.string.update_version_version_code, app.oldVersion, app.oldCode, app.version, app.versionCode)
		view.icon.setImageDrawable(view.context.packageManager.getApplicationIcon(app.packageName))
		view.action_one.text = getString(R.string.action_install)
		if (app.loading) {
			view.progress.visibility = View.VISIBLE
			view.action_one.visibility = View.INVISIBLE
		} else {
			view.progress.visibility = View.INVISIBLE
			view.action_one.visibility = View.VISIBLE
			view.action_one.text = getString(R.string.action_install)
			view.action_one.setOnClickListener { if (app.url.endsWith("apk")) downloadAndInstall(app) else launchUrl(app.url) }
		}
		view.source.setColorFilter(view.context.getAccentColor(), PorterDuff.Mode.MULTIPLY)
		view.source.setImageResource(app.source)
	}

	private fun downloadAndInstall(app: AppUpdate) = ioScope.launch {
		activity?.let { activity ->
			updatesViewModel.setLoading(app.id, true)
			installer.downloadAsync(activity, app.url) { _, _ -> updatesViewModel.setLoading(app.id, true) }.await().fold(
				onSuccess = { installer.install(activity, it, app.id) },
				onFailure = {
					updatesViewModel.setLoading(app.id, false)
					mainViewModel.snackbar.postValue(it.message)
				}
			)
		}
	}

}