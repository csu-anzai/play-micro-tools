@Library("21re") _
gen.init()

publish = false
if(env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'v0.6') {
    publish = true
}

node {
    checkout scm

    sbtBuild([cmds: "clean +compile +test"])

    if(publish) {
      sbtBuild([cmds: "+publish"])
    }
}
