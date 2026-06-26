#include "../src/crypto/BcryptVerifier.h"

#include <QCoreApplication>
#include <iostream>

int main(int argc, char *argv[])
{
    QCoreApplication app(argc, argv);
    const QString fixed = QStringLiteral(
        "$2a$10$ogDPlVsaKYNLzcKYZ/x7gOR4jXlMgI9bADOQtRLpMqasX/yW1ZrFO");
    const QString rehash = vpn::BcryptVerifier::rehash(QStringLiteral("admin123"), fixed);
    const bool ok = vpn::BcryptVerifier::matches(QStringLiteral("admin123"), fixed);
    std::cout << "rehash=" << rehash.toUtf8().constData() << std::endl;
    std::cout << "match=" << (ok ? "1" : "0") << std::endl;
    return ok ? 0 : 1;
}
