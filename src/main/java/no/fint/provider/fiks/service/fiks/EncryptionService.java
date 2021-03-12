package no.fint.provider.fiks.service.fiks;

import no.fint.provider.fiks.SvarUtConfiguration;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSAESOAEPparams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OutputEncryptor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Service
public class EncryptionService {

    private final ASN1ObjectIdentifier cmsEncryptionAlgorithm;
    private final AlgorithmIdentifier keyEncryptionScheme;
    private final SvarUtConfiguration svarUtConfiguration;

    public EncryptionService(SvarUtConfiguration svarUtConfiguration) {
        this.svarUtConfiguration = svarUtConfiguration;
        Security.addProvider(new BouncyCastleProvider());
        this.keyEncryptionScheme = this.rsaesOaepIdentifier();
        this.cmsEncryptionAlgorithm = CMSAlgorithm.AES256_CBC;
    }

    private AlgorithmIdentifier rsaesOaepIdentifier() {
        AlgorithmIdentifier hash = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256, DERNull.INSTANCE);
        AlgorithmIdentifier mask = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1, hash);
        AlgorithmIdentifier pSource = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_pSpecified, new DEROctetString(new byte[0]));
        RSAESOAEPparams parameters = new RSAESOAEPparams(hash, mask, pSource);
        return new AlgorithmIdentifier(PKCSObjectIdentifiers.id_RSAES_OAEP, parameters);
    }

    public byte[] encrypt(byte[] bytes) {
        try {
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(Files.newInputStream(svarUtConfiguration.getCertificate()));
            JceKeyTransRecipientInfoGenerator e = (new JceKeyTransRecipientInfoGenerator(certificate, this.keyEncryptionScheme)).setProvider("BC");
            CMSEnvelopedDataGenerator envelopedDataGenerator = new CMSEnvelopedDataGenerator();
            envelopedDataGenerator.addRecipientInfoGenerator(e);
            OutputEncryptor contentEncryptor = (new JceCMSContentEncryptorBuilder(this.cmsEncryptionAlgorithm)).build();

            CMSEnvelopedData cmsData = envelopedDataGenerator.generate(new CMSProcessableByteArray(bytes), contentEncryptor);
            return cmsData.getEncoded();
        } catch (CertificateEncodingException var7) {
            throw new RuntimeException("Certificate Encoding error", var7);
        } catch (CMSException var8) {
            throw new RuntimeException("Unable to create Cryptographic Message Syntax", var8);
        } catch (IOException var9) {
            throw new RuntimeException(var9);
        } catch (CertificateException e) {
            throw new RuntimeException("Unable to read certificate from " + svarUtConfiguration.getCertificate(), e);
        }
    }

}
