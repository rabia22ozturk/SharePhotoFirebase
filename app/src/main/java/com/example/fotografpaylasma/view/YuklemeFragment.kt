package com.example.fotografpaylasma.view

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.example.fotografpaylasma.databinding.FragmentYuklemeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.util.UUID


class YuklemeFragment : Fragment() {

    private var _binding: FragmentYuklemeBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private var secilenGorsel: Uri? = null
    private var secilenBitmap: Bitmap? = null
    private lateinit var auth : FirebaseAuth
    private lateinit var storage : FirebaseStorage
    private lateinit var db :FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        storage = Firebase.storage
        db = Firebase.firestore
        registerLauncher()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentYuklemeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.yukleButton.setOnClickListener { yukleTiklandi(it) }
        binding.imageView2.setOnClickListener { gorselSec(it) }
    }

    fun yukleTiklandi(view: View){

        val uuid = UUID.randomUUID()
        val gorselAdi = "${uuid}.jpg"

        val reference = storage.reference
        val gorselReferansi = reference.child("images").child(gorselAdi)
        if (secilenGorsel != null){
            gorselReferansi.putFile(secilenGorsel!!).addOnSuccessListener {uploadTask ->
                //url'yi alma işlemini yapacağız
                gorselReferansi.downloadUrl.addOnSuccessListener {uri->

                    if (auth.currentUser!=null){
                        val dowloadUrl = uri.toString()
                        //println(dowloadUrl)
                        //veri tabanına kayıt yapmamaız gerekiyor
                        val postMap = hashMapOf<String,Any>()
                        postMap.put("downlandUrl",dowloadUrl)
                        postMap.put("email",auth.currentUser!!.email.toString())
                        postMap.put("comment",binding.commentText.text.toString())
                        postMap.put("date",Timestamp.now())

                        db.collection("Posts").add(postMap).addOnSuccessListener {documentReference->
                            //veri database'e yüklenmiş oluyor
                            val action = YuklemeFragmentDirections.actionYuklemeFragmentToFeedFragment4()
                            Navigation.findNavController(view).navigate(action)
                        }.addOnFailureListener {exception->
                            Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
                        }

                    }

                }
            }.addOnFailureListener { exception->
                Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
            }
        }

    }

    fun gorselSec(view: View){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {


            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //izin verilmmeiş izin istemmeiz gerekiyor
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        READ_MEDIA_IMAGES
                    )
                ) {
                    //snackbar göstermemiz lazım . kullanıcıdan neden izin istediğimizi söylememiz bir kez daha söylememiz lazım
                    Snackbar.make(
                        view,
                        "Galeriye Ulaşıp Görsel Seçmemiz Lazım!!",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("İzin Ver", View.OnClickListener {
                        //izin isteyeceğiz
                        permissionLauncher.launch(READ_MEDIA_IMAGES)
                    }).show()
                } else {
                    //izin isteyeceğiz
                    permissionLauncher.launch(READ_MEDIA_IMAGES)
                }

            } else {
                //izin verilmiş galeriye girebilirim
                val intentToGallery =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }


        } else {


            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //izin verilmmeiş izin istemmeiz gerekiyor
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        READ_EXTERNAL_STORAGE
                    )
                ) {
                    //snackbar göstermemiz lazım . kullanıcıdan neden izin istediğimizi söylememiz bir kez daha söylememiz lazım
                    Snackbar.make(
                        view,
                        "Galeriye gitmek için izin vermeniz gerekiyor!",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("İzin Ver", View.OnClickListener {
                        //izin isteyeceğiz
                        permissionLauncher.launch(READ_EXTERNAL_STORAGE)
                    }).show()
                } else {
                    //izin isteyeceğiz
                    permissionLauncher.launch(READ_EXTERNAL_STORAGE)
                }

            } else {
                //izin verilmiş galeriye girebilirim
                val intentToGallery =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }


        }


    }

    private fun registerLauncher() {

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val intentFromResult = result.data
                    if (intentFromResult != null) {
                        secilenGorsel = intentFromResult.data
                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                val source = ImageDecoder.createSource(
                                    requireActivity().contentResolver,
                                    secilenGorsel!!
                                )
                                secilenBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView2.setImageBitmap(secilenBitmap)
                            } else {
                                secilenBitmap = MediaStore.Images.Media.getBitmap(
                                    requireActivity().contentResolver,
                                    secilenGorsel
                                )
                                binding.imageView2.setImageBitmap(secilenBitmap)
                            }
                        } catch (e: Exception) {
                           e.printStackTrace()
                        }
                    }
                }
            }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    //izin verildi
                    //galeriye gidebiliriz
                    val intentToGallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGallery)
                } else {
                    //izin verilmedi
                    Toast.makeText(requireContext(), "İzin Verilmedi!!", Toast.LENGTH_LONG).show()
                }
            }



    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}