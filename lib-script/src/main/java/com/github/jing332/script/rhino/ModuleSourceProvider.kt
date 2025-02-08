package com.github.jing332.script.rhino

import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider
import java.io.File

class ModuleSourceProvider : UrlModuleSourceProvider(File.listRoots().map { it.toURI() }, null) {
}