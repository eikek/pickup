# Pickup - a simple backup solution

## Description

Pickup is a simple personal backup solution that distributes your
backup data among a known set of _peers_. Today many people use NAS
devices or similiar things and have plenty of space. Why not keep your
friends backup? In return, your friends can keep your backup. You
always know where your data is and you can control whose data you
store on your device.

It relies on well known tools:

- [duplicity](http://duplicity.nongnu.org/) Creates backup archives
  from your data and transfers it to your peers
- [GnuPG](https://gnupg.org/) Encrypts your data with a password

These tools exists on most GNU/Linux distributions. _Pickup_ is mostly
a web ui ontop of duplicity while it also runs the periodic sync jobs
and provides SFTP/SCP endpoints.

While the main idea is to backup files between pickup instances, you
can define other targets via a corresponding url.

## How it works

When starting pickup, it starts 3 server:

- A “personal” SSH (SFTP/SCP) server you can connect to, by default at
  port `12021`. Use it to push your files you want to backup. This is
  optional.
- A “remote” SSH (SFTP/SCP) for your peers to connect to, by default
  at port `24042`. Your peers are sending their backup files via this
  channel.
- A web server for managing pickup.

After first startup, you must setup pickup. There is a key pair
generated that is used for authenticating the SSH connections. You
also need to provide a password. This is used to encrypt your
data. You need to keep it safe. Without this password, your backups at
your peers are not readable.

You can push all your important files to the first, "personal", SSH
server. They will be stored as is. You need to authenticate using the
private SSH key that you can get from the admin webapp. Alternatively,
you can use other tools like SAMBA or NFS. Pickup just knows one
directory that it backs up.

In the admin app, add a peer to the list of outgoing peers specifing
the remote url, like `somedomain.com:24042`. This is the remote SSH
connection of another pickup instance. At the other pickup instance,
add a new peer to the list of incoming peers. You need to add the
**device id** and the **public SSH key** of your pickup instance.

Pickup will use the `duplicity` tool to backup or restore your data
and transfer it to outgoing peers. Your data will be encrypted using
the password provided during setup using `gnupg`.


## Running pickup

There are two packages provided: a `zip` file and a `deb` package. The
executable `pickup-admin` starts the web server and you can point your
browser to `http://localhost:9101/app/index.html`.

On first access this will show the setup page. It asks for a
password. This password is used to encrypt the backup data. It is
_stored in plain text_ in the pickup database. This is necessary,
because `duplicity` requires access to existing backups when doing
incremental backups. It will also generate SSH keys for the SFTP
endpoints.

If there is one argument, it is assumed to be the configuration file.

If there are the following two or three arguments:

```
[config-file] setup <password>
```

it will only run the setup and exit. This can be used to run the setup
without the webui. If you also want to specify a different config, add
it as first argument.

After the setup is done, you can add incoming and outgoing peers and
run your backups.


## Configuring

You can pass an argument to `pickup-admin` which will be used as the
configuration file. For details about the format, see [its
documentation](https://github.com/lightbend/config/blob/master/HOCON.md).
The default config is below.

``` conf
pickup {

  transfer {
    working-dir = "."

    # The gpg executable.
    gpg-cmd = "gpg"

    # The duplicity executable.
    duplicity-cmd = "duplicity"

    # The ssh-keyscan executable. This is used to manage the
    # known_hosts file.
    keyscan-cmd = "ssh-keyscan"

    # The arguments to duplicity when doing backups.
    #
    # The url and options for transport are added by pickup.
    backup-args = [ "--full-if-older-than", "1M" ]

    # After each backup run, a cleanup run is performed to not grow
    # unbounded.
    cleanup-args = [ "remove-all-but-n-full", "2" ]

    # Additional arguments passed to restore command.
    restore-args = [ "-vI" ]

    # The personal sftp endpoint can be used to copy your important
    # data to backup. Pickup will backup the directory specified as
    # root.
    personal {
      # Whether to enable this SFTP endpoint. If you have other means
      # to push data to this directory below, you can disable this.
      enable = true

      # The root directory to this SFTP endpoint. This is also the
      # directory that is backed up. Restores are also copied into
      # this folder.
      root = ${pickup.transfer.working-dir}"/ssh-personal"

      # The host and port to bind the sftp server to.
      host = "localhost"
      port = 12021

      # A default user for logging in.
      default-user = "backup"

      # A connection string exposed to users. The above info specifies
      # where the sftp server binds to. This string is shown to users
      # so that they can connect. This can be different, if for
      # example DNS is used.
      connection-uri = ${pickup.transfer.personal.default-user}"@"${pickup.transfer.personal.host}":"${pickup.transfer.personal.port}
    }

    # The "remote" sftp endpoint. This is where other peers connect
    # and sent their encrypted backup data.
    remote {
      root = ${pickup.transfer.working-dir}"/ssh-remote"
      host = "localhost"
      port = 24042

      # A connection string exposed to users. The above info specifies
      # where the sftp server binds to. This string is shown to users
      # so that they can connect. This can be different, if for
      # example DNS is used.
      connection-uri = ${pickup.transfer.remote.host}":"${pickup.transfer.remote.port}
    }

    db {
      pool-size = 10
    }

    # SMTP settings used to send notification mails.  This is only
    # necessary to fully supply if you send mails to arbirtrary
    # mailboxes. If, for example, you only need to send to yourself,
    # just add your mail as the `sender` below. It many cases this
    # should work
    smtp {
      host = ""
      port = 0
      user = ""
      password = ""
      start-tls = false
      use-ssl = false
      sender = "noreply@localhost"
    }

    # Enable to get notified via email when a backup fails.
    notify-mail {
      enable = false
      recipients = [ "recipient@somedomain.com" ]
    }
  }

  http {
    app-name = "Pickup"

    bind {
      host = "localhost"
      port = 9101
    }
  }
}
```
