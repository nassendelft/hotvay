
actual fun configToRegistrations(config: ConfigFile) = config.registrations
    .map { Registration(KeyEvent(it.key), it.description, it.action) }

private val ConfigRegistration.action: Action get() = when {
    this.appBundleId != null -> ApplicationAction(this.appBundleId)
    this.command != null -> ExecuteAction(this.command)
    else -> error("Could not find action")
}
