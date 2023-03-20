package com.github.mauricio.async.db.mysql

import com.github.mauricio.async.db.Configuration
import org.specs2.mutable.Specification

/**
 * To run this spec you have to use the Vagrant file provided with the base project and you have to start MySQL there.
 * The expected MySQL version is 5.1.73. Make sure the bootstrap.sh script is run, if it isn't, manually run it
 * yourself.
 */
class ClientPluginAuthDisabledSpec extends Specification with ConnectionHelper
